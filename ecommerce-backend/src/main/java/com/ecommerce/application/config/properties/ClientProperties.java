package com.ecommerce.application.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientProperties {

    @NotNull
    @Min(1)
    private Integer otpTtlSeconds;
}
