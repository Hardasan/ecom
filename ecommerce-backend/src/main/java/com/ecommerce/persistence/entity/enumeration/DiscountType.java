package com.ecommerce.persistence.entity.enumeration;

public enum DiscountType {

    /**
     * {@code value} is a percentage (1-100) of the eligible subtotal, optionally capped by {@code maxDiscountAmount}.
     */
    PERCENTAGE,

    /**
     * {@code value} is a flat money amount subtracted from the eligible subtotal (never more than it).
     */
    FIXED_AMOUNT
}
