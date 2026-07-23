package com.ecommerce.application.integration.product;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductRemoveImageITest extends AbstractProductITest {

    @Test
    void remove_main_image_sets_it_to_null() throws Exception {
        Long id = createProductAndGetId("main-img-to-remove");
        uploadMainImage(id);

        mockMvc.perform(delete("/api/products/{id}/images", id)
                        .param("type", "MAIN")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mainImage", nullValue()));
    }

    @Test
    void remove_other_image_by_id_deletes_only_that_entry() throws Exception {
        Long id = createProductAndGetId("multi-img-product");

        Long firstId = uploadOtherImageAndGetId(id, "first");
        Long secondId = uploadOtherImageAndGetId(id, "second");

        mockMvc.perform(delete("/api/products/{id}/images", id)
                        .param("type", "OTHER")
                        .param("imageId", firstId.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        MvcResult getResult = mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherImages", hasSize(1)))
                .andReturn();

        Long remainingId = json(getResult).path("otherImages").get(0).get("id").asLong();
        assertEquals(secondId, remainingId, "only the second image should remain after removing the first");
    }

    @Test
    void remove_image_without_auth_returns_401() throws Exception {
        Long id = createProductAndGetId("del-img-no-auth");

        mockMvc.perform(delete("/api/products/{id}/images", id)
                        .param("type", "MAIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void remove_other_image_without_image_id_returns_400() throws Exception {
        Long id = createProductAndGetId("del-other-no-id");

        mockMvc.perform(delete("/api/products/{id}/images", id)
                        .param("type", "OTHER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void remove_other_image_with_unknown_image_id_returns_400() throws Exception {
        Long id = createProductAndGetId("del-other-unknown-id");
        uploadOtherImageAndGetId(id, "only");

        mockMvc.perform(delete("/api/products/{id}/images", id)
                        .param("type", "OTHER")
                        .param("imageId", "999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void remove_image_with_user_role_returns_403() throws Exception {
        Long id = createProductAndGetId("del-img-user-forbidden");
        uploadMainImage(id);

        mockMvc.perform(delete("/api/products/{id}/images", id)
                        .param("type", "MAIN")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
