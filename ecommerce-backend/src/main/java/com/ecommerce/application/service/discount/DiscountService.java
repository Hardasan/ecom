package com.ecommerce.application.service.discount;

import com.ecommerce.application.api.dto.discount.*;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.CartItem;
import com.ecommerce.persistence.entity.Discount;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.enumeration.DiscountScope;
import com.ecommerce.persistence.entity.enumeration.DiscountType;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import com.ecommerce.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private static final BigDecimal MAX_PERCENTAGE = BigDecimal.valueOf(100);

    private final DiscountRepository discountRepository;
    private final DiscountMapper discountMapper;
    private final DiscountCalculator discountCalculator;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;

    // ---------------------------------------------------------------------------------------------
    // Admin CRUD
    // ---------------------------------------------------------------------------------------------

    @Transactional
    public DiscountResponseDto create(CreateDiscountRequestDto requestDto) {
        String code = normalizeCode(requestDto.getCode());
        if (discountRepository.existsByCodeIgnoreCase(code)) {
            throw new EcommerceException(ECOMErrorType.DISCOUNT_CODE_ALREADY_EXISTS);
        }
        validateConfig(requestDto.getType(), requestDto.getValue(), requestDto.getMaxDiscountAmount(),
                requestDto.getScope(), requestDto.getProductIds(), requestDto.getCategoryIds());

        Discount discount = new Discount();
        discountMapper.apply(requestDto, discount);
        discount.setCode(code);
        applyScopeTargets(discount, requestDto.getScope(), requestDto.getProductIds(), requestDto.getCategoryIds());
        return discountMapper.toResponseDto(discountRepository.save(discount));
    }

    @Transactional(readOnly = true)
    public DiscountListResponseDto getAll() {
        List<DiscountResponseDto> discounts = discountRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(discountMapper::toResponseDto)
                .toList();
        return new DiscountListResponseDto(discounts);
    }

    @Transactional(readOnly = true)
    public DiscountResponseDto getById(Long id) {
        return discountMapper.toResponseDto(findOrThrow(id));
    }

    @Transactional
    public DiscountResponseDto update(Long id, UpdateDiscountRequestDto requestDto) {
        Discount discount = findOrThrow(id);
        String code = normalizeCode(requestDto.getCode());
        if (discountRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new EcommerceException(ECOMErrorType.DISCOUNT_CODE_ALREADY_EXISTS);
        }
        validateConfig(requestDto.getType(), requestDto.getValue(), requestDto.getMaxDiscountAmount(),
                requestDto.getScope(), requestDto.getProductIds(), requestDto.getCategoryIds());

        discountMapper.apply(requestDto, discount);
        discount.setCode(code);
        applyScopeTargets(discount, requestDto.getScope(), requestDto.getProductIds(), requestDto.getCategoryIds());
        return discountMapper.toResponseDto(discountRepository.save(discount));
    }

    @Transactional
    public void delete(Long id) {
        discountRepository.delete(findOrThrow(id));
    }

    // ---------------------------------------------------------------------------------------------
    // Customer-facing: preview + the checkout seam
    // ---------------------------------------------------------------------------------------------

    /**
     * Estimates what a code would take off the caller's <em>current</em> cart, without redeeming it.
     * Purely a UI aid — checkout recomputes authoritatively from the order it actually places.
     */
    @Transactional(readOnly = true)
    public DiscountPreviewResponseDto preview(Long userId, String code) {
        // Read-only estimate: no lock, no redemption. Checkout recomputes authoritatively under a lock.
        Discount discount = discountRepository.findByCodeIgnoreCase(normalizeCode(code))
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.DISCOUNT_CODE_INVALID));
        assertUsable(discount, userId);

        List<DiscountableLine> lines = resolveCartLines(userId);
        BigDecimal itemsCost = lines.stream()
                .map(DiscountableLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DiscountComputation computation = discountCalculator.compute(discount, lines);
        return new DiscountPreviewResponseDto(discount.getCode(), discount.getType(), itemsCost,
                computation.eligibleSubtotal(), computation.amount(), itemsCost.subtract(computation.amount()));
    }

    /**
     * Redeems a code for an order under a pessimistic row lock, then returns what it takes off.
     *
     * <p>Must run inside the checkout transaction (it does — {@code CheckoutService.placeOrder} is
     * {@code @Transactional}). {@link DiscountRepository#findByCodeForUpdate} takes the discount row's
     * {@code FOR UPDATE} lock, held until that transaction commits, so the limit checks, the counter
     * increment and the subsequent order insert cannot interleave with another redemption of the same
     * code. This is the one serialization point that makes both the global and the per-user limits
     * exact under concurrency — a losing contender blocks here and then observes the winner's counter
     * (and committed order) before its own checks run.
     */
    public AppliedDiscount redeemForOrder(String code, Long userId, List<DiscountableLine> lines) {
        Discount discount = discountRepository.findByCodeForUpdate(normalizeCode(code))
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.DISCOUNT_CODE_INVALID));
        assertUsable(discount, userId);

        DiscountComputation computation = discountCalculator.compute(discount, lines);

        // Safe under the row lock we hold: no other redemption can read a stale usage_count.
        discount.setUsageCount(discount.getUsageCount() + 1);
        return new AppliedDiscount(discount.getId(), discount.getCode(), computation.amount());
    }

    // ---------------------------------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------------------------------

    /**
     * Runs every gate that does not depend on the cart contents (expiry, global + per-user limits).
     */
    private void assertUsable(Discount discount, Long userId) {
        if (discount.getExpiresAt() != null && discount.getExpiresAt().before(new Date())) {
            throw new EcommerceException(ECOMErrorType.DISCOUNT_EXPIRED);
        }
        if (discount.getUsageLimit() != null && discount.getUsageCount() >= discount.getUsageLimit()) {
            throw new EcommerceException(ECOMErrorType.DISCOUNT_USAGE_LIMIT_REACHED);
        }
        if (discount.getPerUserLimit() != null
                && orderRepository.countActiveByDiscountAndUser(discount.getId(),
                userId) >= discount.getPerUserLimit()) {
            throw new EcommerceException(ECOMErrorType.DISCOUNT_USAGE_LIMIT_REACHED);
        }
    }

    private void validateConfig(DiscountType type, BigDecimal value, BigDecimal maxDiscountAmount,
            DiscountScope scope, Set<Long> productIds, Set<Long> categoryIds) {
        if (type == DiscountType.PERCENTAGE && value.compareTo(MAX_PERCENTAGE) > 0) {
            throw new EcommerceException(ECOMErrorType.DISCOUNT_INVALID_CONFIG);
        }
        if (type == DiscountType.FIXED_AMOUNT && maxDiscountAmount != null) {
            // A cap only makes sense for a percentage; a flat amount is already its own ceiling.
            throw new EcommerceException(ECOMErrorType.DISCOUNT_INVALID_CONFIG);
        }
        switch (scope) {
            case PRODUCTS -> requireExistingTargets(productIds, productRepository::findAllById,
                    ECOMErrorType.PRODUCT_NOT_FOUND);
            case CATEGORIES -> requireExistingTargets(categoryIds, categoryRepository::findAllById,
                    ECOMErrorType.CATEGORY_NOT_FOUND);
            case ALL -> { /* no targets */ }
        }
    }

    private <T> void requireExistingTargets(Set<Long> ids, Function<Set<Long>, List<T>> loader,
            ECOMErrorType notFound) {
        if (ids == null || ids.isEmpty()) {
            throw new EcommerceException(ECOMErrorType.DISCOUNT_INVALID_CONFIG);
        }
        if (loader.apply(ids).size() != ids.size()) {
            throw new EcommerceException(notFound);
        }
    }

    private void applyScopeTargets(Discount discount, DiscountScope scope, Set<Long> productIds,
            Set<Long> categoryIds) {
        discount.getProductIds().clear();
        discount.getCategoryIds().clear();
        if (scope == DiscountScope.PRODUCTS) {
            discount.getProductIds().addAll(productIds);
        } else if (scope == DiscountScope.CATEGORIES) {
            discount.getCategoryIds().addAll(categoryIds);
        }
    }

    private List<DiscountableLine> resolveCartLines(Long userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = cartItems.stream().map(CartItem::getProductId).distinct().toList();
        Map<Long, Product> products = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<DiscountableLine> lines = new ArrayList<>();
        for (CartItem item : cartItems) {
            Product product = products.get(item.getProductId());
            if (product == null) {
                continue; // stale cart line for a removed product — ignore for the estimate
            }
            BigDecimal unitPrice = effectiveUnitPrice(product, item.getVariantType(), item.getVariantValue());
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            lines.add(new DiscountableLine(product.getId(), product.getCategoryId(), product.getSubCategoryId(),
                    lineTotal, item.getQuantity()));
        }
        return lines;
    }

    private BigDecimal effectiveUnitPrice(Product product, VariantType variantType, String variantValue) {
        return product.getPrices().stream()
                .filter(price -> Objects.equals(variantValue, price.getVariantValue()))
                .findFirst()
                .or(() -> product.getPrices().stream().findFirst())
                .map(price -> price.getDiscountPrice() != null ? price.getDiscountPrice() : price.getPrice())
                .orElse(BigDecimal.ZERO);
    }

    private Discount findOrThrow(Long id) {
        return discountRepository.findById(id)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.DISCOUNT_NOT_FOUND));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
