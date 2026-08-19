package com.ecommerce.application.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GeoITest extends AbstractIntegrationITest {

    @Test
    void public_provinces_returns_all_iran_provinces() throws Exception {
        mockMvc.perform(get("/api/geo/provinces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provinces", hasSize(31)))
                .andExpect(jsonPath("$.provinces[?(@.code=='TEHRAN')].name", hasItem("تهران")));
    }

    @Test
    void public_cities_returns_cities_for_province() throws Exception {
        mockMvc.perform(get("/api/geo/cities").param("province", "TEHRAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cities.length()", greaterThan(1)))
                .andExpect(jsonPath("$.cities[?(@.name=='تهران')]", hasSize(1)));
    }

    @Test
    void cities_without_province_returns_400() throws Exception {
        mockMvc.perform(get("/api/geo/cities"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
