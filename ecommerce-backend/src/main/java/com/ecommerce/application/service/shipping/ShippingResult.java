package com.ecommerce.application.service.shipping;

import com.ecommerce.persistence.entity.enumeration.ShippingZone;

import java.math.BigDecimal;

/**
 * Outcome of a shipping calculation: the resolved zone and the cost for it.
 */
public record ShippingResult(ShippingZone zone, BigDecimal cost) {
}
