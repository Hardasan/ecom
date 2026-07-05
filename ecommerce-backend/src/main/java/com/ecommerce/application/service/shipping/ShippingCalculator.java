package com.ecommerce.application.service.shipping;

import com.ecommerce.application.config.properties.ShippingProperties;
import com.ecommerce.application.config.properties.ShippingProperties.ZoneRate;
import com.ecommerce.persistence.entity.enumeration.Province;
import com.ecommerce.persistence.entity.enumeration.ShippingZone;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resolves the postal shipping zone for a destination and the cost for the order's total weight,
 * using the configurable {@link ShippingProperties} tariff.
 */
@Component
@RequiredArgsConstructor
public class ShippingCalculator {

    private final ShippingProperties shippingProperties;

    public ShippingResult calculate(Province destination, int totalWeightGram) {
        ShippingZone zone = resolveZone(destination);
        ZoneRate rate = shippingProperties.getRates().get(zone);
        if (rate == null) {
            throw new IllegalStateException("No shipping rate configured for zone " + zone);
        }
        BigDecimal cost = totalWeightGram <= shippingProperties.getWeightThresholdGram()
                ? rate.getUpToThreshold()
                : rate.getOverThreshold();
        return new ShippingResult(zone, cost);
    }

    private ShippingZone resolveZone(Province destination) {
        Province origin = shippingProperties.getOriginProvince();
        if (destination == origin) {
            return ShippingZone.INTRA_PROVINCE;
        }
        List<Province> neighbours = shippingProperties.getAdjacency().get(origin);
        if (neighbours != null && neighbours.contains(destination)) {
            return ShippingZone.ADJACENT_PROVINCE;
        }
        return ShippingZone.NON_ADJACENT_PROVINCE;
    }
}
