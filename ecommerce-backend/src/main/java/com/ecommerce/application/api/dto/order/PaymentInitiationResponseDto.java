package com.ecommerce.application.api.dto.order;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentInitiationResponseDto {

    private String paymentReference;

    private String redirectUrl;
}
