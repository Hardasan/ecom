package com.ecommerce.application.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClientConfigITest extends AbstractIntegrationITest {

    @Test
    void public_client_config_returns_otp_ttl() throws Exception {
        mockMvc.perform(get("/api/client-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpTtlSeconds").value(120));
    }
}
