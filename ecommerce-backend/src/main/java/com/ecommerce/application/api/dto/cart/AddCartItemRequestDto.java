package com.ecommerce.application.api.dto.cart;

import com.ecommerce.persistence.entity.enumeration.VariantType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCartItemRequestDto {

    @NotNull
    private Long productId;

    private VariantType variantType;

    private String variantValue;

    @NotNull
    @Positive
    private Integer quantity;
}
