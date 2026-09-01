package com.ecommerce.application.api.dto.order;

import java.math.BigDecimal;

/**
 * A non-persisting price preview for the current cart shipped to a chosen address: the same numbers
 * {@code POST /api/checkout} would charge (items + shipping), so the checkout screen can show the
 * real total — including shipping — before the order is placed.
 */
public record CheckoutQuoteResponseDto(
        BigDecimal itemsCost,
        BigDecimal shippingCost,
        BigDecimal totalCost) {
}
