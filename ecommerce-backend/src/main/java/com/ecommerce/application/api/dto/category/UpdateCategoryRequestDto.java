package com.ecommerce.application.api.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCategoryRequestDto {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String localName;

    private Long parentId;
}
