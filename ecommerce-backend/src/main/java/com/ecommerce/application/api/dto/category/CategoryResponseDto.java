package com.ecommerce.application.api.dto.category;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryResponseDto {

    private Long id;
    private String name;
    private String localName;
    private Long parentId;

    // Number of ACTIVE products under this category (its own + its sub-categories'), filled on the
    // list endpoint so the storefront category cards can show «N کالا». Null on single-item reads.
    private Integer productCount;
}
