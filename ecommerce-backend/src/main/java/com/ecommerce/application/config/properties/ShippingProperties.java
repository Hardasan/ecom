package com.ecommerce.application.config.properties;

import com.ecommerce.persistence.entity.enumeration.Province;
import com.ecommerce.persistence.entity.enumeration.ShippingZone;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Postal shipping tariff configuration ({@code app.shipping}).
 *
 * <p>The cost of an order's shipment is a function of (a) the destination
 * {@link ShippingZone} relative to {@link #originProvince} and (b) whether the order's total
 * weight stays within {@link #weightThresholdGram}. All amounts are configurable so the tariff
 * can be updated without code changes.
 */
@Getter
@Setter
public class ShippingProperties {

    /** Province the goods are dispatched from. */
    private Province originProvince;

    /** Weight (grams) up to and including which the lighter tariff applies. */
    private int weightThresholdGram;

    /** For each province, the provinces considered adjacent to it (همجوار). */
    private Map<Province, List<Province>> adjacency = new EnumMap<>(Province.class);

    /** Tariff per shipping zone. */
    private Map<ShippingZone, ZoneRate> rates = new EnumMap<>(ShippingZone.class);

    @Getter
    @Setter
    public static class ZoneRate {

        /** Cost when total weight is within {@link ShippingProperties#weightThresholdGram}. */
        private BigDecimal upToThreshold;

        /** Cost when total weight exceeds {@link ShippingProperties#weightThresholdGram}. */
        private BigDecimal overThreshold;
    }
}
