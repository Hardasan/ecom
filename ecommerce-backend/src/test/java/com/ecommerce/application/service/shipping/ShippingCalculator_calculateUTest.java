package com.ecommerce.application.service.shipping;

import com.ecommerce.application.config.properties.ShippingProperties;
import com.ecommerce.application.config.properties.ShippingProperties.ZoneRate;
import com.ecommerce.persistence.entity.enumeration.Province;
import com.ecommerce.persistence.entity.enumeration.ShippingZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShippingCalculator_calculateUTest {

    private static final BigDecimal INTRA_LIGHT = BigDecimal.valueOf(183000);
    private static final BigDecimal INTRA_HEAVY = BigDecimal.valueOf(570000);
    private static final BigDecimal ADJACENT_LIGHT = BigDecimal.valueOf(260000);
    private static final BigDecimal ADJACENT_HEAVY = BigDecimal.valueOf(600000);
    private static final BigDecimal NON_ADJACENT_LIGHT = BigDecimal.valueOf(282200);
    private static final BigDecimal NON_ADJACENT_HEAVY = BigDecimal.valueOf(620000);

    private ShippingCalculator calculator;

    @BeforeEach
    void setUp() {
        ShippingProperties properties = new ShippingProperties();
        properties.setOriginProvince(Province.TEHRAN);
        properties.setWeightThresholdGram(1000);

        Map<Province, List<Province>> adjacency = new EnumMap<>(Province.class);
        adjacency.put(Province.TEHRAN, List.of(Province.ALBORZ, Province.QOM, Province.QAZVIN));
        properties.setAdjacency(adjacency);

        Map<ShippingZone, ZoneRate> rates = new EnumMap<>(ShippingZone.class);
        rates.put(ShippingZone.INTRA_PROVINCE, rate(INTRA_LIGHT, INTRA_HEAVY));
        rates.put(ShippingZone.ADJACENT_PROVINCE, rate(ADJACENT_LIGHT, ADJACENT_HEAVY));
        rates.put(ShippingZone.NON_ADJACENT_PROVINCE, rate(NON_ADJACENT_LIGHT, NON_ADJACENT_HEAVY));
        properties.setRates(rates);

        calculator = new ShippingCalculator(properties);
    }

    @Test
    void same_province_light_parcel_is_intra_zone_light_rate() {
        ShippingResult result = calculator.calculate(Province.TEHRAN, 500);

        assertEquals(ShippingZone.INTRA_PROVINCE, result.zone());
        assertEquals(INTRA_LIGHT, result.cost());
    }

    @Test
    void same_province_heavy_parcel_is_intra_zone_heavy_rate() {
        ShippingResult result = calculator.calculate(Province.TEHRAN, 1500);

        assertEquals(ShippingZone.INTRA_PROVINCE, result.zone());
        assertEquals(INTRA_HEAVY, result.cost());
    }

    @Test
    void adjacent_province_uses_adjacent_zone_rate() {
        ShippingResult result = calculator.calculate(Province.ALBORZ, 500);

        assertEquals(ShippingZone.ADJACENT_PROVINCE, result.zone());
        assertEquals(ADJACENT_LIGHT, result.cost());
    }

    @Test
    void non_adjacent_province_uses_non_adjacent_zone_rate() {
        ShippingResult result = calculator.calculate(Province.FARS, 500);

        assertEquals(ShippingZone.NON_ADJACENT_PROVINCE, result.zone());
        assertEquals(NON_ADJACENT_LIGHT, result.cost());
    }

    @Test
    void weight_exactly_at_threshold_uses_light_rate() {
        ShippingResult result = calculator.calculate(Province.FARS, 1000);

        assertEquals(NON_ADJACENT_LIGHT, result.cost());
    }

    @Test
    void weight_one_gram_over_threshold_uses_heavy_rate() {
        ShippingResult result = calculator.calculate(Province.FARS, 1001);

        assertEquals(NON_ADJACENT_HEAVY, result.cost());
    }

    private ZoneRate rate(BigDecimal light, BigDecimal heavy) {
        ZoneRate zoneRate = new ZoneRate();
        zoneRate.setUpToThreshold(light);
        zoneRate.setOverThreshold(heavy);
        return zoneRate;
    }
}
