package com.ecommerce.application.api.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Shipment details captured by warehouse staff when an order is handed to the courier
 * (PROCESSING -> SENDING). Both fields are required so a shipped order always carries a
 * carrier and a tracking number the buyer can follow.
 */
@Getter
@Setter
public class ShipOrderRequestDto {

    @NotBlank
    @Size(max = 64)
    private String carrier;

    @NotBlank
    @Size(max = 128)
    private String trackingNumber;
}
