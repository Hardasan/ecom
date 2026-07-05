package com.ecommerce.application.api.dto.order;

import com.ecommerce.persistence.entity.enumeration.VariantType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuestItemRequestDto {

    @NotNull
    private Long productId;

    @NotNull
    private VariantType variantType;

    @NotNull
    @Positive
    private Integer quantity;
}
