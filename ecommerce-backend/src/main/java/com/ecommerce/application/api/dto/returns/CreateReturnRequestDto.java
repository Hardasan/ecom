package com.ecommerce.application.api.dto.returns;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Body to open a return request. The شبا is optional here — it defaults to the shopper's saved
 * profile IBAN (the return flow never asks for it); an override is accepted for flexibility.
 */
@Getter
@Setter
public class CreateReturnRequestDto {

    @NotNull
    private Long orderId;

    @Size(max = 1000)
    private String note;

    @Pattern(regexp = "^IR[0-9]{24}$", message = "IBAN must be IR followed by 24 digits")
    private String iban;

    @Valid
    @NotEmpty
    private List<CreateReturnItemDto> items;
}
