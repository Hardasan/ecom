package com.ecommerce.application.api.dto.discount;

import com.ecommerce.persistence.entity.enumeration.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Result of a successful discount preview: what the code would take off the caller's current cart.
 * A code that cannot be applied returns the corresponding {@code DISCOUNT_*} error instead.
 */
@Getter
@AllArgsConstructor
public class DiscountPreviewResponseDto {

    /**
     * The normalised (stored) code that matched.
     */
    private String code;

    private DiscountType type;

    /**
     * Cart items subtotal before the discount.
     */
    private BigDecimal itemsCost;

    /**
     * Subtotal of the items the code applies to (equals {@code itemsCost} for whole-cart codes).
     */
    private BigDecimal eligibleSubtotal;

    /**
     * Money the code takes off.
     */
    private BigDecimal discountAmount;

    /**
     * {@code itemsCost - discountAmount} (shipping is charged separately at checkout).
     */
    private BigDecimal newItemsCost;
}
