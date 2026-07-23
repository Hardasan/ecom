package com.ecommerce.application.api.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentConfirmRequestDto {

    @NotBlank
    private String paymentReference;
}
