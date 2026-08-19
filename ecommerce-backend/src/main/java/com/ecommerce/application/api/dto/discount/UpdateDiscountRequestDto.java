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
 * Admin request to fully replace a discount's configuration. Same semantics as
 * {@link CreateDiscountRequestDto}; {@code usageCount} is server-owned and never touched here.
 */
@Getter
@Setter
public class UpdateDiscountRequestDto {

    @NotBlank
    private String code;

    @NotNull
    private DiscountType type;

    @NotNull
    @Positive
    private BigDecimal value;

    @Positive
    private BigDecimal maxDiscountAmount;

    @Positive
    private BigDecimal minimumCartAmount;

    @NotNull
    private DiscountScope scope;

    private Set<Long> productIds;

    private Set<Long> categoryIds;

    private Date expiresAt;

    @Positive
    private Integer usageLimit;

    @Positive
    private Integer perUserLimit;
}
