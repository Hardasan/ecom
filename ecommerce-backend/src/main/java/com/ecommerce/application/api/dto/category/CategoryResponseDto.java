package com.ecommerce.application.api.dto.category;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategoryResponseDto {

    private Long id;
    private String name;
    private String localName;
    private Long parentId;
}
