package com.ecommerce.application.api.dto.cart;

import com.ecommerce.application.api.dto.product.ProductImageDto;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CartItemResponseDto {

    private Long id;

    private Long productId;

    private String productName;

    private String productCode;

    private ProductImageDto mainImage;

    private VariantType variantType;

    private String variantValue;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal discountPrice;

    private BigDecimal effectivePrice;

    private BigDecimal lineTotal;
}
