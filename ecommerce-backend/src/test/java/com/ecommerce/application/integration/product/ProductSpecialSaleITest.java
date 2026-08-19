package com.ecommerce.application.integration.product;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductSpecialSaleITest extends AbstractProductITest {

    @Test
    void special_sale_returns_up_to_five_active_in_stock_products() throws Exception {
        createProductAndGetId("sale-1");
        createProductAndGetId("sale-2");
        createProductAndGetId("sale-3");
        createProductAndGetId("sale-4");
        createProductAndGetId("sale-5");
        createProductAndGetId("sale-6");

        mockMvc.perform(get("/api/products/special-sale"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.products", hasSize(5)));
    }

    @Test
    void special_sale_is_public() throws Exception {
        createProductAndGetId("public-sale");

        mockMvc.perform(get("/api/products/special-sale"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(1)));
    }
}
