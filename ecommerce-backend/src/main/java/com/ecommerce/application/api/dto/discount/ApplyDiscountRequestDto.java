package com.ecommerce.application.api.dto.discount;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for previewing a discount code against the caller's current cart.
 */
@Getter
@Setter
public class ApplyDiscountRequestDto {

    @NotBlank
    private String code;
}
