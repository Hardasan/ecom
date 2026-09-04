package com.ecommerce.application.api.dto.product;

import com.ecommerce.persistence.entity.enumeration.InventoryStatus;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class SearchProductResponseDto {

    private Long id;

    private String code;

    private String name;

    private String localName;

    private Long categoryId;

    private Long subCategoryId;

    private Long brandId;

    private String url;

    private VariantType variantType;

    private List<PriceDto> prices;

    private String shortDescription;

    private ProductImageDto mainImage;

    private InventoryStatus inventoryStatus;

    private ProductStatus status;

    private Integer inventoryCount;

    // Review aggregate for card/list display. Derived on read from PUBLISHED product_review rows
    // (same "derive, don't store" model as the review summary); ratingCount 0 ⇒ no rating badge.
    private Double averageRating;

    private Long ratingCount;

    private Date createdAt;

    private Date updatedAt;
}
