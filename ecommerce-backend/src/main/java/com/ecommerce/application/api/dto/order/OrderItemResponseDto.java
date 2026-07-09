package com.ecommerce.application.api.dto.order;

import com.ecommerce.persistence.entity.enumeration.VariantType;
import com.ecommerce.persistence.entity.enumeration.VariantValue;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderItemResponseDto {

    private Long id;

    private Long productId;

    private String productName;

    private String productCode;

    private VariantType variantType;

    private VariantValue variantValue;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal discountPrice;

    private BigDecimal lineTotal;
}
