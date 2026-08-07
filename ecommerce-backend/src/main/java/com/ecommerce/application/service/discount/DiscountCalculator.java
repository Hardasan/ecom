package com.ecommerce.application.service.discount;

import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.Discount;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

/**
 * Pure discount math — no persistence, no time, no counters. Given a code's configuration and the
 * cart lines, it decides which lines the code covers, enforces the minimum-purchase gate, and
 * computes the money to take off:
 *
 * <ul>
 *   <li>the base is the <b>eligible subtotal</b> — the sum of in-scope line totals ({@code ALL} = whole cart);</li>
 *   <li>{@code PERCENTAGE} = eligibleSubtotal × value%, then capped by {@code maxDiscountAmount} if set;</li>
 *   <li>{@code FIXED_AMOUNT} = value;</li>
 *   <li>the result is clamped to the eligible subtotal (never negative, never more than the items are worth)
 *       and rounded to 2 decimals, {@code HALF_UP}.</li>
 * </ul>
 *
 * <p>Expiry, activation and usage limits are <em>not</em> checked here — those need the entity, the
 * clock and the redemption counters, and live in {@code DiscountService}.
 */
@Component
public class DiscountCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MONEY_SCALE = 2;

    public DiscountComputation compute(Discount discount, List<DiscountableLine> lines) {
        BigDecimal eligibleSubtotal = eligibleSubtotal(discount, lines);
        if (eligibleSubtotal.signum() <= 0) {
            throw new EcommerceException(ECOMErrorType.DISCOUNT_NOT_APPLICABLE);
        }

        BigDecimal minimum = discount.getMinimumCartAmount();
        if (minimum != null && eligibleSubtotal.compareTo(minimum) < 0) {
            throw new EcommerceException(ECOMErrorType.DISCOUNT_MINIMUM_NOT_MET, minimum);
        }

        BigDecimal raw = switch (discount.getType()) {
            case PERCENTAGE -> capPercentage(
                    eligibleSubtotal.multiply(discount.getValue()).divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP),
                    discount.getMaxDiscountAmount());
            case FIXED_AMOUNT -> discount.getValue();
        };

        BigDecimal amount = raw.min(eligibleSubtotal).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return new DiscountComputation(amount, eligibleSubtotal);
    }

    private BigDecimal eligibleSubtotal(Discount discount, List<DiscountableLine> lines) {
        return lines.stream()
                .filter(line -> isEligible(discount, line))
                .map(DiscountableLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isEligible(Discount discount, DiscountableLine line) {
        return switch (discount.getScope()) {
            case ALL -> true;
            case PRODUCTS -> discount.getProductIds().contains(line.productId());
            case CATEGORIES -> matchesCategory(discount.getCategoryIds(), line);
        };
    }

    private boolean matchesCategory(Set<Long> categoryIds, DiscountableLine line) {
        return categoryIds.contains(line.categoryId())
                || (line.subCategoryId() != null && categoryIds.contains(line.subCategoryId()));
    }

    private BigDecimal capPercentage(BigDecimal computed, BigDecimal max) {
        return max == null ? computed : computed.min(max);
    }
}
