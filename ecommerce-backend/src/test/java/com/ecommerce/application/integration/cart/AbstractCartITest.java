package com.ecommerce.application.integration.cart;

import com.ecommerce.application.api.dto.cart.AddCartItemRequestDto;
import com.ecommerce.application.api.dto.product.CreateProductRequestDto;
import com.ecommerce.application.api.dto.product.PriceDto;
import com.ecommerce.application.integration.AbstractIntegrationITest;
import com.ecommerce.persistence.entity.enumeration.InventoryStatus;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class AbstractCartITest extends AbstractIntegrationITest {

    static final VariantType DEFAULT_VARIANT_TYPE = VariantType.COLOR;
    static final String DEFAULT_VARIANT_VALUE = "#FF0000";

    @Autowired
    private PasswordEncoder passwordEncoder;

    String adminToken;
    String userToken;
    Long categoryId;

    @BeforeEach
    void setupCartFixtures() throws Exception {
        // product (and therefore cart_item) is cleared via the FK cascade chain.
        jdbcTemplate.execute("TRUNCATE TABLE category, brand RESTART IDENTITY CASCADE");

        String adminMobile = "09100000000";
        jdbcTemplate.update(
                "INSERT INTO app_user (first_name, last_name, username, mobile, password, role, is_enabled, is_registered) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "Admin", "User", adminMobile, adminMobile,
                passwordEncoder.encode("Admin123!"), "ROLE_ADMIN", true, true);
        adminToken = login(adminMobile, "Admin123!");

        userToken = registerAndLogin(newMobile());

        categoryId = jdbcTemplate.queryForObject(
                "INSERT INTO category (name) VALUES ('Electronics') RETURNING id", Long.class);
    }

    // ---------------------------------------------------------------------------------------------
    // Product fixtures
    // ---------------------------------------------------------------------------------------------

    Long createProduct(String url, int inventory, ProductStatus status, String... variantValues) throws Exception {
        CreateProductRequestDto req = new CreateProductRequestDto();
        req.setCategoryId(categoryId);
        req.setUrl(url);
        req.setName("Test Product " + url);
        req.setStatus(status);
        req.setInventoryStatus(InventoryStatus.IN_STOCK);
        req.setInventoryCount(inventory);
        req.setVariantType(DEFAULT_VARIANT_TYPE);

        List<PriceDto> prices = new ArrayList<>();
        BigDecimal base = BigDecimal.valueOf(100);
        String[] values = variantValues.length == 0 ? new String[]{DEFAULT_VARIANT_VALUE} : variantValues;
        for (String variantValue : values) {
            PriceDto price = new PriceDto();
            price.setPrice(base);
            price.setVariantValue(variantValue);
            prices.add(price);
            base = base.add(BigDecimal.valueOf(10));
        }
        req.setPrices(prices);

        MvcResult result = mockMvc.perform(multipart("/api/products")
                        .part(jsonPart("data", req))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("id").asLong();
    }

    Long createActiveProduct(String url, int inventory, String... variantValues) throws Exception {
        return createProduct(url, inventory, ProductStatus.ACTIVE, variantValues);
    }

    org.springframework.mock.web.MockPart jsonPart(String name, Object body) throws Exception {
        org.springframework.mock.web.MockPart part =
                new org.springframework.mock.web.MockPart(name, objectMapper.writeValueAsBytes(body));
        part.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return part;
    }

    // ---------------------------------------------------------------------------------------------
    // Cart HTTP helpers
    // ---------------------------------------------------------------------------------------------

    ResultActions getCart(String token) throws Exception {
        return mockMvc.perform(withAuth(get("/api/cart"), token));
    }

    ResultActions addItem(String token, Long productId, String variantValue, int quantity) throws Exception {
        AddCartItemRequestDto req = new AddCartItemRequestDto();
        req.setProductId(productId);
        req.setVariantType(DEFAULT_VARIANT_TYPE);
        req.setVariantValue(variantValue == null ? null : variantValue);
        req.setQuantity(quantity);
        return mockMvc.perform(withAuth(post("/api/cart/items"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }

    ResultActions incrementItem(String token, long itemId) throws Exception {
        return mockMvc.perform(withAuth(post("/api/cart/items/{itemId}/increment", itemId), token));
    }

    ResultActions decrementItem(String token, long itemId) throws Exception {
        return mockMvc.perform(withAuth(post("/api/cart/items/{itemId}/decrement", itemId), token));
    }

    ResultActions updateQuantity(String token, long itemId, int quantity) throws Exception {
        return mockMvc.perform(withAuth(patch("/api/cart/items/{itemId}", itemId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("quantity", quantity))));
    }

    ResultActions removeItem(String token, long itemId) throws Exception {
        return mockMvc.perform(withAuth(delete("/api/cart/items/{itemId}", itemId), token));
    }

    ResultActions clearCart(String token) throws Exception {
        return mockMvc.perform(withAuth(delete("/api/cart"), token));
    }

    long addItemAndGetId(String token, Long productId, String variantValue, int quantity) throws Exception {
        MvcResult result = addItem(token, productId, variantValue, quantity)
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = json(result).get("items");
        for (JsonNode itemNode : items) {
            if (itemNode.get("productId").asLong() == productId
                    && DEFAULT_VARIANT_TYPE.name().equals(itemNode.get("variantType").asText())
                    && variantValue.equalsIgnoreCase(itemNode.get("variantValue").asText())) {
                return itemNode.get("id").asLong();
            }
        }
        throw new IllegalStateException("Added cart item not found in response: " + items);
    }
}
