package com.ecommerce.application.integration;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserIbanITest extends AbstractIntegrationITest {

    private static final String VALID_IBAN = "IR062960000000100324200001";

    @Test
    void get_iban_is_null_before_set() throws Exception {
        String token = registerAndLogin(newMobile());

        mockMvc.perform(withAuth(get("/api/user/iban"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iban").value(nullValue()));
    }

    @Test
    void put_iban_stores_and_get_returns_it() throws Exception {
        String token = registerAndLogin(newMobile());

        mockMvc.perform(withAuth(put("/api/user/iban"), token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("iban", VALID_IBAN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iban").value(VALID_IBAN));

        mockMvc.perform(withAuth(get("/api/user/iban"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iban").value(VALID_IBAN));
    }

    @Test
    void put_iban_can_be_updated() throws Exception {
        String token = registerAndLogin(newMobile());
        String second = "IR120170000000123456789012";

        mockMvc.perform(withAuth(put("/api/user/iban"), token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("iban", VALID_IBAN))))
                .andExpect(status().isOk());

        mockMvc.perform(withAuth(put("/api/user/iban"), token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("iban", second))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iban").value(second));
    }

    @Test
    void put_iban_rejects_invalid_format() throws Exception {
        String token = registerAndLogin(newMobile());

        mockMvc.perform(withAuth(put("/api/user/iban"), token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("iban", "IR123"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(withAuth(put("/api/user/iban"), token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("iban", "DE89370400440532013000"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void put_iban_rejects_blank() throws Exception {
        String token = registerAndLogin(newMobile());

        mockMvc.perform(withAuth(put("/api/user/iban"), token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("iban", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void iban_endpoints_require_auth() throws Exception {
        mockMvc.perform(get("/api/user/iban"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/user/iban")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("iban", VALID_IBAN))))
                .andExpect(status().isUnauthorized());
    }
}
