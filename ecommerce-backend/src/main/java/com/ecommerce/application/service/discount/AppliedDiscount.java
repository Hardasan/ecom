package com.ecommerce.application.service.discount;

import java.math.BigDecimal;

/**
 * A validated, computed discount ready to be snapshotted onto an order: which code (by id and
 * normalised text) and how much it takes off. Redemption is claimed separately by the caller.
 */
public record AppliedDiscount(Long discountId, String code, BigDecimal amount) {
}
