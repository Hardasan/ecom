package com.ecommerce.application.service.order;

import com.ecommerce.application.api.dto.address.AddressResponseDto;
import com.ecommerce.application.api.dto.order.CheckoutRequestDto;
import com.ecommerce.application.api.dto.order.GuestCheckoutRequestDto;
import com.ecommerce.application.api.dto.order.GuestItemRequestDto;
import com.ecommerce.application.api.dto.order.OrderResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.application.config.properties.CheckoutProperties;
import com.ecommerce.application.service.address.AddressService;
import com.ecommerce.application.service.shipping.ShippingCalculator;
import com.ecommerce.application.service.shipping.ShippingResult;
import com.ecommerce.persistence.entity.AppUser;
import com.ecommerce.persistence.entity.CartItem;
import com.ecommerce.persistence.entity.Order;
import com.ecommerce.persistence.entity.OrderItem;
import com.ecommerce.persistence.entity.Price;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.UserAddress;
import com.ecommerce.persistence.entity.embeddable.AddressSnapshot;
import com.ecommerce.persistence.entity.embeddable.ProductSnapshot;
import com.ecommerce.persistence.entity.enumeration.OrderStatus;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.UserRole;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import com.ecommerce.persistence.repository.AppUserRepository;
import com.ecommerce.persistence.repository.CartItemRepository;
import com.ecommerce.persistence.repository.OrderRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import com.ecommerce.persistence.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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

    @Transactional
    public OrderResponseDto checkout(Long userId, CheckoutRequestDto requestDto) {
        UserAddress address = userAddressRepository.findByIdAndUserId(requestDto.getAddressId(), userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.ADDRESS_NOT_FOUND));

        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        List<OrderLineSpec> lines = resolveCartLines(cartItems);

        Order order = placeOrder(userId, address, lines);
        cartItemRepository.deleteByUserId(userId);
        return orderMapper.toResponseDto(order);
    }

    @Transactional
    public OrderResponseDto guestCheckout(GuestCheckoutRequestDto requestDto) {
        AppUser guest = resolveGuestUser(requestDto);
        Long userId = guest.getId();

        AddressResponseDto created = addressService.create(userId, requestDto.getAddress());
        UserAddress address = userAddressRepository.findByIdAndUserId(created.getId(), userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.ADDRESS_NOT_FOUND));

        List<OrderLineSpec> lines = resolveGuestLines(requestDto.getItems());
        return orderMapper.toResponseDto(placeOrder(userId, address, lines));
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> listOrders(Long userId) {
        return orderRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(orderMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.ORDER_NOT_FOUND));
        return orderMapper.toResponseDto(order);
    }

    private Order placeOrder(Long userId, UserAddress address, List<OrderLineSpec> lines) {
        if (lines.isEmpty()) {
            throw new EcommerceException(ECOMErrorType.EMPTY_CART);
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        applyAddressSnapshot(address, order);

        BigDecimal itemsCost = BigDecimal.ZERO;
        int totalWeightGram = 0;
        for (OrderLineSpec line : lines) {
            BigDecimal effectivePrice = line.discountPrice() != null ? line.discountPrice() : line.unitPrice();
            BigDecimal lineTotal = effectivePrice.multiply(BigDecimal.valueOf(line.quantity()));

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

            itemsCost = itemsCost.add(lineTotal);
            totalWeightGram += line.weightGram() * line.quantity();
        }

        ShippingResult shipping = shippingCalculator.calculate(order.getShippingAddress().getProvince(), totalWeightGram);
        order.setItemsCost(itemsCost);
        order.setTotalWeightGram(totalWeightGram);
        order.setShippingZone(shipping.zone());
        order.setShippingCost(shipping.cost());
        order.setTotalCost(itemsCost.add(shipping.cost()));

        for (OrderLineSpec line : lines) {
            int updated = productRepository.decrementInventory(line.productId(), line.quantity());
            if (updated == 0) {
                throw new EcommerceException(ECOMErrorType.INSUFFICIENT_STOCK);
            }
        }

        order.setReservedUntil(Date.from(Instant.now().plus(checkoutProperties.getReservationTimeout())));

        return orderRepository.save(order);
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
                    product.getWeightGram() == null ? 0 : product.getWeightGram()));
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
                    product.getWeightGram() == null ? 0 : product.getWeightGram()));
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
                                  int weightGram) {
    }
}
