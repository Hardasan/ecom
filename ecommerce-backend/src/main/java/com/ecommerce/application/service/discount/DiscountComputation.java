package com.ecommerce.application.service.discount;

import java.math.BigDecimal;

/**
 * Output of {@link DiscountCalculator}: the money to take off and the eligible (in-scope) subtotal
 * it was computed from.
 */
public record DiscountComputation(BigDecimal amount, BigDecimal eligibleSubtotal) {
}
