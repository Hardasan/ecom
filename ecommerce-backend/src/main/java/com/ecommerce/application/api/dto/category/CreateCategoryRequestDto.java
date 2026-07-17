package com.ecommerce.application.api.dto.category;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCategoryRequestDto {

    @NotEmpty
    private String name;

    private String localName;

    private Long parentId;
}
