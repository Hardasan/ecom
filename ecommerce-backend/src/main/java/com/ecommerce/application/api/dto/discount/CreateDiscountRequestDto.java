package com.ecommerce.application.api.dto.discount;

import com.ecommerce.persistence.entity.enumeration.DiscountScope;
import com.ecommerce.persistence.entity.enumeration.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

/**
 * Admin request to create a discount code. Field-level rules are enforced here; cross-field rules
 * (percentage &le; 100, scope requires matching target ids, cap only for percentage) are validated
 * in {@code DiscountService} and surface as {@code DISCOUNT_INVALID_CONFIG}.
 */
@Getter
@Setter
public class CreateDiscountRequestDto {

    @NotBlank
    private String code;

    @NotNull
    private DiscountType type;

    /**
     * Percentage (1-100) for {@code PERCENTAGE}; a flat amount for {@code FIXED_AMOUNT}.
     */
    @NotNull
    @Positive
    private BigDecimal value;

    /**
     * Optional cap for a percentage discount.
     */
    @Positive
    private BigDecimal maxDiscountAmount;

    /**
     * Optional minimum eligible subtotal before the code applies.
     */
    @Positive
    private BigDecimal minimumCartAmount;

    @NotNull
    private DiscountScope scope;

    /**
     * Required (non-empty) when {@code scope=PRODUCTS}; ignored otherwise.
     */
    private Set<Long> productIds;

    /**
     * Required (non-empty) when {@code scope=CATEGORIES}; ignored otherwise.
     */
    private Set<Long> categoryIds;

    /**
     * Optional expiry; null = never expires.
     */
    private Date expiresAt;

    /**
     * Optional global redemption cap; null = unlimited.
     */
    @Positive
    private Integer usageLimit;

    /**
     * Optional per-user redemption cap; null = unlimited.
     */
    @Positive
    private Integer perUserLimit;
}
