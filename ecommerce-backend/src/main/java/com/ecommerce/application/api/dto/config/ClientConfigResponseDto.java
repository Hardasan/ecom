package com.ecommerce.application.api.dto.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClientConfigResponseDto {

    private Integer otpTtlSeconds;
}
