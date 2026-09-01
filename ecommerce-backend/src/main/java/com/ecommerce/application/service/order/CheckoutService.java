package com.ecommerce.application.service.order;

import com.ecommerce.application.api.dto.address.AddressResponseDto;
import com.ecommerce.application.api.dto.order.CheckoutQuoteResponseDto;
import com.ecommerce.application.api.dto.order.CheckoutRequestDto;
import com.ecommerce.application.api.dto.order.GuestCheckoutRequestDto;
import com.ecommerce.application.api.dto.order.GuestItemRequestDto;
import com.ecommerce.application.api.dto.order.OrderResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.application.config.properties.CheckoutProperties;
import com.ecommerce.application.service.address.AddressService;
import com.ecommerce.application.service.discount.AppliedDiscount;
import com.ecommerce.application.service.discount.DiscountService;
import com.ecommerce.application.service.discount.DiscountableLine;
import com.ecommerce.application.service.shipping.ShippingCalculator;
import com.ecommerce.application.service.shipping.ShippingResult;
import com.ecommerce.persistence.entity.*;
import com.ecommerce.persistence.entity.embeddable.AddressSnapshot;
import com.ecommerce.persistence.entity.embeddable.ProductSnapshot;
import com.ecommerce.persistence.entity.enumeration.OrderStatus;
import com.ecommerce.persistence.entity.enumeration.PaymentMethod;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.Province;
import com.ecommerce.persistence.entity.enumeration.ShippingZone;
import com.ecommerce.persistence.entity.enumeration.UserRole;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import com.ecommerce.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserAddressRepository userAddressRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ShippingCalculator shippingCalculator;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AddressService addressService;
    private final CheckoutProperties checkoutProperties;
    private final DiscountService discountService;

    @Transactional
    public OrderResponseDto checkout(Long userId, CheckoutRequestDto requestDto) {
        UserAddress address = userAddressRepository.findByIdAndUserId(requestDto.getAddressId(), userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.ADDRESS_NOT_FOUND));

        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        List<OrderLineSpec> lines = resolveCartLines(cartItems);

        Order order = placeOrder(userId, address, lines, requestDto.getDiscountCode(),
                resolvePaymentMethod(requestDto.getPaymentMethod()));
        cartItemRepository.deleteByUserId(userId);
        return toDto(order);
    }

    /**
     * Price preview for the current cart shipped to one of the caller's addresses — the same
     * items + shipping {@link #checkout} would charge, computed without creating an order or
     * touching inventory. Lets the checkout screen show the real total (with shipping) up front.
     */
    @Transactional(readOnly = true)
    public CheckoutQuoteResponseDto quote(Long userId, Long addressId) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.ADDRESS_NOT_FOUND));

        List<OrderLineSpec> lines = resolveCartLines(cartItemRepository.findByUserId(userId));
        if (lines.isEmpty()) {
            throw new EcommerceException(ECOMErrorType.EMPTY_CART);
        }
        CostBreakdown costs = computeCosts(lines, address.getProvince());
        return new CheckoutQuoteResponseDto(costs.itemsCost(), costs.shippingCost(),
                costs.itemsCost().add(costs.shippingCost()));
    }

    private PaymentMethod resolvePaymentMethod(PaymentMethod requested) {
        return requested == null ? PaymentMethod.ONLINE : requested;
    }

    @Transactional
    public OrderResponseDto guestCheckout(GuestCheckoutRequestDto requestDto) {
        AppUser guest = resolveGuestUser(requestDto);
        Long userId = guest.getId();

        AddressResponseDto created = addressService.create(userId, requestDto.getAddress());
        UserAddress address = userAddressRepository.findByIdAndUserId(created.getId(), userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.ADDRESS_NOT_FOUND));

        List<OrderLineSpec> lines = resolveGuestLines(requestDto.getItems());
        return toDto(placeOrder(userId, address, lines, requestDto.getDiscountCode(), PaymentMethod.ONLINE));
    }

    private OrderResponseDto toDto(Order order) {
        List<Long> productIds = order.getItems().stream()
                .map(item -> item.getProduct().getProductId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Product> products = productIds.isEmpty()
                ? Map.of()
                : productRepository.findAllById(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, p -> p));
        OrderResponseDto dto = orderMapper.toResponseDto(order);
        orderMapper.attachMainImages(dto, products);
        return dto;
    }

    private Order placeOrder(Long userId, UserAddress address, List<OrderLineSpec> lines, String discountCode,
            PaymentMethod paymentMethod) {
        if (lines.isEmpty()) {
            throw new EcommerceException(ECOMErrorType.EMPTY_CART);
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.RESERVED);
        order.setPaymentMethod(paymentMethod);
        applyAddressSnapshot(address, order);

        List<DiscountableLine> discountableLines = new ArrayList<>();
        for (OrderLineSpec line : lines) {
            BigDecimal lineTotal = effectivePrice(line).multiply(BigDecimal.valueOf(line.quantity()));

            OrderItem orderItem = new OrderItem();
            ProductSnapshot productSnapshot = orderItem.getProduct();
            productSnapshot.setProductId(line.productId());
            productSnapshot.setProductName(line.productName());
            productSnapshot.setProductCode(line.productCode());
            orderItem.setVariantType(line.variantType());
            orderItem.setVariantValue(line.variantValue());
            orderItem.setQuantity(line.quantity());
            orderItem.setUnitPrice(line.unitPrice());
            orderItem.setDiscountPrice(line.discountPrice());
            orderItem.setLineTotal(lineTotal);
            order.addItem(orderItem);

            discountableLines.add(new DiscountableLine(line.productId(), line.categoryId(),
                    line.subCategoryId(), lineTotal, line.quantity()));
        }

        // Validate/claim the discount before computing shipping: an invalid code must abort the
        // checkout before any further work.
        BigDecimal discountAmount = applyDiscount(order, userId, discountCode, discountableLines);
        CostBreakdown costs = computeCosts(lines, order.getShippingAddress().getProvince());

        order.setItemsCost(costs.itemsCost());
        order.setTotalWeightGram(costs.totalWeightGram());
        order.setShippingZone(costs.shippingZone());
        order.setShippingCost(costs.shippingCost());
        order.setTotalCost(costs.itemsCost().subtract(discountAmount).add(costs.shippingCost()));

        for (OrderLineSpec line : lines) {
            int updated = productRepository.decrementInventory(line.productId(), line.quantity());
            if (updated == 0) {
                throw new EcommerceException(ECOMErrorType.INSUFFICIENT_STOCK);
            }
        }

        // Online orders hold their stock only for a limited window; if unpaid, ReservationReleaseService
        // fails them and restores stock. Cash-on-delivery orders are placed for good and shipped later,
        // so they never expire (reservedUntil stays null → excluded from findExpiredReservations).
        if (paymentMethod != PaymentMethod.CASH_ON_DELIVERY) {
            order.setReservedUntil(Date.from(Instant.now().plus(checkoutProperties.getReservationTimeout())));
        }

        return orderRepository.save(order);
    }

    /** The per-unit price actually charged for a line: its discount price when set, else the unit price. */
    private BigDecimal effectivePrice(OrderLineSpec line) {
        return line.discountPrice() != null ? line.discountPrice() : line.unitPrice();
    }

    /**
     * Items subtotal, total weight, and shipping for a set of order lines to a destination province —
     * the pre-discount cost math shared by {@link #placeOrder} and {@link #quote} so a quote can never
     * drift from what checkout actually charges.
     */
    private CostBreakdown computeCosts(List<OrderLineSpec> lines, Province province) {
        BigDecimal itemsCost = BigDecimal.ZERO;
        int totalWeightGram = 0;
        for (OrderLineSpec line : lines) {
            itemsCost = itemsCost.add(effectivePrice(line).multiply(BigDecimal.valueOf(line.quantity())));
            totalWeightGram += line.weightGram() * line.quantity();
        }
        ShippingResult shipping = shippingCalculator.calculate(province, totalWeightGram);
        return new CostBreakdown(itemsCost, totalWeightGram, shipping.zone(), shipping.cost());
    }

    private record CostBreakdown(BigDecimal itemsCost, int totalWeightGram, ShippingZone shippingZone,
            BigDecimal shippingCost) {
    }

    /**
     * Validates and applies a discount code, snapshotting it onto the order and claiming a redemption
     * slot under a pessimistic lock held for this checkout transaction (see
     * {@code DiscountService.redeemForOrder}). No code (null/blank) leaves the order at zero discount.
     *
     * @return the money taken off (zero when no code was applied)
     */
    private BigDecimal applyDiscount(Order order, Long userId, String discountCode,
            List<DiscountableLine> lines) {
        if (discountCode == null || discountCode.isBlank()) {
            return BigDecimal.ZERO;
        }
        AppliedDiscount applied = discountService.redeemForOrder(discountCode, userId, lines);
        order.setDiscountId(applied.discountId());
        order.setDiscountCode(applied.code());
        order.setDiscountAmount(applied.amount());
        return applied.amount();
    }

    private List<OrderLineSpec> resolveCartLines(List<CartItem> cartItems) {
        Map<VariantKey, Integer> quantities = new LinkedHashMap<>();
        for (CartItem item : cartItems) {
            quantities.merge(new VariantKey(item.getProductId(), item.getVariantType(), item.getVariantValue()),
                    item.getQuantity(), Integer::sum);
        }

        List<OrderLineSpec> lines = new ArrayList<>();
        for (Map.Entry<VariantKey, Integer> entry : quantities.entrySet()) {
            Product product = findProductOrThrow(entry.getKey().productId());
            requirePurchasable(product);
            Price price = findVariantPriceOrThrow(product, entry.getKey().variantType(), entry.getKey().variantValue());
            lines.add(new OrderLineSpec(product.getId(), product.getName(), product.getCode(),
                    entry.getKey().variantType(), entry.getKey().variantValue(), entry.getValue(),
                    price.getPrice(), price.getDiscountPrice(),
                    product.getWeightGram() == null ? 0 : product.getWeightGram(),
                    product.getCategoryId(), product.getSubCategoryId()));
        }
        return lines;
    }

    private List<OrderLineSpec> resolveGuestLines(List<GuestItemRequestDto> items) {
        Map<VariantKey, Integer> quantities = new LinkedHashMap<>();
        for (GuestItemRequestDto item : items) {
            quantities.merge(new VariantKey(item.getProductId(), item.getVariantType(), item.getVariantValue()),
                    item.getQuantity(), Integer::sum);
        }

        List<OrderLineSpec> lines = new ArrayList<>();
        for (Map.Entry<VariantKey, Integer> entry : quantities.entrySet()) {
            Product product = findProductOrThrow(entry.getKey().productId());
            requirePurchasable(product);
            Price price = findVariantPriceOrThrow(product, entry.getKey().variantType(), entry.getKey().variantValue());
            lines.add(new OrderLineSpec(product.getId(), product.getName(), product.getCode(),
                    entry.getKey().variantType(), entry.getKey().variantValue(), entry.getValue(),
                    price.getPrice(), price.getDiscountPrice(),
                    product.getWeightGram() == null ? 0 : product.getWeightGram(),
                    product.getCategoryId(), product.getSubCategoryId()));
        }
        return lines;
    }

    private AppUser resolveGuestUser(GuestCheckoutRequestDto requestDto) {
        Optional<AppUser> existing = appUserRepository.findByMobile(requestDto.getMobile());
        if (existing.isPresent() && Boolean.TRUE.equals(existing.get().getIsRegistered())) {
            throw new EcommerceException(ECOMErrorType.USER_ALREADY_EXISTS);
        }
        AppUser user = existing.orElseGet(AppUser::new);
        user.setFirstName(requestDto.getFirstName());
        user.setLastName(requestDto.getLastName());
        user.setUsername(requestDto.getMobile());
        user.setMobile(requestDto.getMobile());
        user.setEmail(requestDto.getEmail());
        user.setNationalId(requestDto.getNationalId());
        if (user.getPassword() == null) {
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        }
        user.setRole(UserRole.ROLE_APP_USER);
        user.setIsEnabled(true);
        user.setIsRegistered(false);
        return appUserRepository.save(user);
    }

    private void applyAddressSnapshot(UserAddress address, Order order) {
        AddressSnapshot snapshot = order.getShippingAddress();
        snapshot.setRecipientFirstName(address.getRecipientFirstName());
        snapshot.setRecipientLastName(address.getRecipientLastName());
        snapshot.setRecipientMobile(address.getRecipientMobile());
        snapshot.setRecipientNationalId(address.getRecipientNationalId());
        snapshot.setProvince(address.getProvince());
        snapshot.setCity(address.getCity());
        snapshot.setPostalCode(address.getPostalCode());
        snapshot.setAddressLine(address.getAddressLine());
        snapshot.setPlaque(address.getPlaque());
        snapshot.setUnit(address.getUnit());
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.PRODUCT_NOT_FOUND));
    }

    private void requirePurchasable(Product product) {
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new EcommerceException(ECOMErrorType.PRODUCT_NOT_AVAILABLE);
        }
    }

    private Price findVariantPriceOrThrow(Product product, VariantType variantType, String variantValue) {
        return product.getPrices().stream()
                .filter(price -> Objects.equals(variantType, product.getVariantType())
                        && Objects.equals(variantValue, price.getVariantValue()))
                .findFirst()
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.PRODUCT_VARIANT_NOT_FOUND));
    }

    private record VariantKey(Long productId, VariantType variantType, String variantValue) {
    }

    private record OrderLineSpec(Long productId, String productName, String productCode,
                                  VariantType variantType, String variantValue, int quantity,
                                  BigDecimal unitPrice, BigDecimal discountPrice,
                                 int weightGram, Long categoryId, Long subCategoryId) {
    }
}
