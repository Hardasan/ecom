package com.ecommerce.application.config.properties;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
public class CheckoutProperties {

    private Duration reservationTimeout = Duration.ofMinutes(30);
}
