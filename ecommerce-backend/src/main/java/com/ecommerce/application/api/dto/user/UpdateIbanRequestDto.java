package com.ecommerce.application.api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateIbanRequestDto {

    @NotBlank
    @Pattern(regexp = "^IR[0-9]{24}$", message = "IBAN must be IR followed by 24 digits")
    private String iban;
}
