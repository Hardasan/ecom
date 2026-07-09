package com.ecommerce.application.integration.product;

import com.ecommerce.application.api.dto.product.CreateProductRequestDto;
import com.ecommerce.application.api.dto.product.PriceDto;
import com.ecommerce.application.integration.AbstractIntegrationITest;
import com.fasterxml.jackson.databind.JsonNode;
import com.ecommerce.persistence.entity.enumeration.InventoryStatus;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import com.ecommerce.persistence.entity.enumeration.VariantValue;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class AbstractProductITest extends AbstractIntegrationITest {

    static final byte[] SAMPLE_IMAGE = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};

    @Autowired
    private PasswordEncoder passwordEncoder;

    String adminToken;
    String userToken;
    Long categoryId;
    Long brandId;

    // Runs after AbstractIntegrationITest.resetState() (parent @BeforeEach runs first in JUnit 5).
    @BeforeEach
    void setupProductFixtures() throws Exception {
        // Cascades to product, product_price, product_other_image via FK chain.
        jdbcTemplate.execute("TRUNCATE TABLE category, brand RESTART IDENTITY CASCADE");

        // Admin users cannot be created via the signup flow — that always assigns ROLE_APP_USER.
        String adminMobile = "09100000000";
        jdbcTemplate.update(
                "INSERT INTO app_user (first_name, last_name, username, mobile, password, role, is_enabled, is_registered) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "Admin", "User", adminMobile, adminMobile,
                passwordEncoder.encode("Admin123!"), "ROLE_ADMIN", true, true);
        adminToken = login(adminMobile, "Admin123!");

        String userMobile = newMobile();
        jdbcTemplate.update(
                "INSERT INTO app_user (first_name, last_name, username, mobile, password, role, is_enabled, is_registered) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "Test", "User", userMobile, userMobile,
                passwordEncoder.encode(DEFAULT_PASSWORD), "ROLE_APP_USER", true, true);
        userToken = login(userMobile, DEFAULT_PASSWORD);

        categoryId = jdbcTemplate.queryForObject(
                "INSERT INTO category (name) VALUES ('Electronics') RETURNING id", Long.class);
        brandId = jdbcTemplate.queryForObject(
                "INSERT INTO brand (name) VALUES ('TestBrand') RETURNING id", Long.class);
    }

    CreateProductRequestDto validRequest(String url) {
        PriceDto price = new PriceDto();
        price.setPrice(BigDecimal.valueOf(100));
        price.setVariantValue(VariantValue.RED);

        CreateProductRequestDto req = new CreateProductRequestDto();
        req.setCategoryId(categoryId);
        req.setUrl(url);
        req.setName("Test Product");
        req.setStatus(ProductStatus.ACTIVE);
        req.setInventoryStatus(InventoryStatus.IN_STOCK);
        req.setInventoryCount(10);
        req.setVariantType(VariantType.COLOR);
        req.setPrices(List.of(price));
        return req;
    }

    MockPart jsonPart(String name, Object body) throws Exception {
        MockPart part = new MockPart(name, objectMapper.writeValueAsBytes(body));
        part.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return part;
    }

    MockPart imagePart(String name) {
        MockPart part = new MockPart(name, "test.jpg", SAMPLE_IMAGE);
        part.getHeaders().setContentType(MediaType.IMAGE_JPEG);
        return part;
    }

    ResultActions multipartCreate(CreateProductRequestDto req, String token) throws Exception {
        return mockMvc.perform(multipart("/api/products")
                .part(jsonPart("data", req))
                .header("Authorization", "Bearer " + token));
    }

    Long createProductAndGetId(String url) throws Exception {
        MvcResult result = multipartCreate(validRequest(url), adminToken)
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("id").asLong();
    }

    void uploadMainImage(Long productId) throws Exception {
        mockMvc.perform(multipart("/api/products/{id}/images", productId)
                        .part(imagePart("image"))
                        .param("type", "MAIN")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    Long uploadOtherImageAndGetId(Long productId, String altText) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/products/{id}/images", productId)
                        .part(imagePart("image"))
                        .param("type", "OTHER")
                        .param("altText", altText)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode others = json(result).path("otherImages");
        return others.get(others.size() - 1).get("id").asLong();
    }
}
