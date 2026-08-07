package com.ecommerce.application.api.dto.discount;

import com.ecommerce.persistence.entity.enumeration.DiscountScope;
import com.ecommerce.persistence.entity.enumeration.DiscountType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

/**
 * Admin-facing view of a discount code, including the server-owned {@code usageCount}.
 */
@Getter
@Setter
public class DiscountResponseDto {

    private Long id;
    private String code;
    private DiscountType type;
    private BigDecimal value;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minimumCartAmount;
    private DiscountScope scope;
    private Set<Long> productIds;
    private Set<Long> categoryIds;
    private Date expiresAt;
    private Integer usageLimit;
    private Integer usageCount;
    private Integer perUserLimit;
    private Date createdAt;
    private Date updatedAt;
}
