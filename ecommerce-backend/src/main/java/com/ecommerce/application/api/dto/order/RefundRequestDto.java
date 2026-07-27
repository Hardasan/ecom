package com.ecommerce.application.api.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundRequestDto {

    @NotBlank
    private String reference;

    @NotBlank
    @Pattern(regexp = "^IR[0-9]{24}$", message = "IBAN must be IR followed by 24 digits")
    private String iban;
}
