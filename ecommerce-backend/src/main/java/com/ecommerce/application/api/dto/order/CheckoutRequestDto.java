package com.ecommerce.application.api.dto.order;

import com.ecommerce.persistence.entity.enumeration.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Authenticated checkout: the order is built from the caller's current cart and shipped to one of
 * their saved addresses. To use a brand-new address, create it via {@code POST /api/addresses}
 * first, then pass its id here.
 */
@Getter
@Setter
public class CheckoutRequestDto {

    @NotNull
    private Long addressId;

    /**
     * Optional discount code to apply. Null/blank ⇒ no discount.
     */
    private String discountCode;

    /**
     * How the order is paid. Null defaults to {@link PaymentMethod#ONLINE} (pay via the gateway
     * before shipping). {@link PaymentMethod#CASH_ON_DELIVERY} places the order without online
     * payment.
     */
    private PaymentMethod paymentMethod;
}
