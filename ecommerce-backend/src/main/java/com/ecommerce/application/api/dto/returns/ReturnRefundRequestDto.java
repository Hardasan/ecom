package com.ecommerce.application.api.dto.returns;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin records the bank transfer that pays back an APPROVED return. The reference is the transfer
 * tracking code; the شبا is optional here because the return already carries the one the shopper
 * asked to be paid to — when present it overrides it (e.g. the shopper gave a wrong/blank شبا).
 */
@Getter
@Setter
public class ReturnRefundRequestDto {

    @NotBlank
    private String reference;

    @Pattern(regexp = "^IR[0-9]{24}$", message = "IBAN must be IR followed by 24 digits")
    private String iban;
}
