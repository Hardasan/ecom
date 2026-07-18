package com.ecommerce.application.integration.wishlist;

import com.ecommerce.application.api.dto.product.CreateProductRequestDto;
import com.ecommerce.application.api.dto.product.PriceDto;
import com.ecommerce.application.api.dto.wishlist.AddWishlistItemRequestDto;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class AbstractWishlistITest extends AbstractIntegrationITest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    static final VariantType DEFAULT_VARIANT_TYPE = VariantType.COLOR;
    static final String DEFAULT_VARIANT_VALUE = "#FF0000";

    String adminToken;
    String userToken;
    String userMobile;
    Long userId;
    Long categoryId;

    @BeforeEach
    void setupWishlistFixtures() throws Exception {
        // product (and therefore wishlist_item) is cleared via the FK cascade chain.
        jdbcTemplate.execute("TRUNCATE TABLE category, brand RESTART IDENTITY CASCADE");

        String adminMobile = "09100000000";
        jdbcTemplate.update(
                "INSERT INTO app_user (first_name, last_name, username, mobile, password, role, is_enabled, is_registered) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "Admin", "User", adminMobile, adminMobile,
                passwordEncoder.encode("Admin123!"), "ROLE_ADMIN", true, true);
        adminToken = login(adminMobile, "Admin123!");

        userMobile = newMobile();
        userToken = registerAndLogin(userMobile);
        userId = userIdOf(userMobile);

        categoryId = jdbcTemplate.queryForObject(
                "INSERT INTO category (name) VALUES ('Electronics') RETURNING id", Long.class);
    }

    // ---------------------------------------------------------------------------------------------
    // Product fixtures
    // ---------------------------------------------------------------------------------------------

    // A wishlist bookmark is product-level, so these fixtures only ever need one priced variant.
    Long createProduct(String url, int inventory, ProductStatus status) throws Exception {
        CreateProductRequestDto req = new CreateProductRequestDto();
        req.setCategoryId(categoryId);
        req.setUrl(url);
        req.setName("Test Product " + url);
        req.setStatus(status);
        req.setInventoryStatus(inventory > 0 ? InventoryStatus.IN_STOCK : InventoryStatus.OUT_OF_STOCK);
        req.setInventoryCount(inventory);
        req.setVariantType(DEFAULT_VARIANT_TYPE);

        PriceDto price = new PriceDto();
        price.setPrice(BigDecimal.valueOf(100));
        price.setVariantValue(DEFAULT_VARIANT_VALUE);
        req.setPrices(new ArrayList<>(List.of(price)));

        MvcResult result = mockMvc.perform(multipart("/api/products")
                        .part(jsonPart("data", req))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("id").asLong();
    }

    Long createActiveProduct(String url, int inventory) throws Exception {
        return createProduct(url, inventory, ProductStatus.ACTIVE);
    }

    org.springframework.mock.web.MockPart jsonPart(String name, Object body) throws Exception {
        org.springframework.mock.web.MockPart part =
                new org.springframework.mock.web.MockPart(name, objectMapper.writeValueAsBytes(body));
        part.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return part;
    }

    // ---------------------------------------------------------------------------------------------
    // Wishlist HTTP helpers
    // ---------------------------------------------------------------------------------------------

    ResultActions getWishlist(String token) throws Exception {
        return mockMvc.perform(withAuth(get("/api/wishlist"), token));
    }

    ResultActions addItem(String token, Long productId) throws Exception {
        AddWishlistItemRequestDto req = new AddWishlistItemRequestDto();
        req.setProductId(productId);
        return mockMvc.perform(withAuth(post("/api/wishlist/items"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }

    ResultActions removeItem(String token, long itemId) throws Exception {
        return mockMvc.perform(withAuth(delete("/api/wishlist/items/{itemId}", itemId), token));
    }

    ResultActions removeByProduct(String token, long productId) throws Exception {
        return mockMvc.perform(withAuth(delete("/api/wishlist/products/{productId}", productId), token));
    }

    ResultActions contains(String token, long productId) throws Exception {
        return mockMvc.perform(withAuth(get("/api/wishlist/products/{productId}", productId), token));
    }

    ResultActions clearWishlist(String token) throws Exception {
        return mockMvc.perform(withAuth(delete("/api/wishlist"), token));
    }

    long addItemAndGetId(String token, Long productId) throws Exception {
        MvcResult result = addItem(token, productId)
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = json(result).get("items");
        for (JsonNode itemNode : items) {
            if (itemNode.get("productId").asLong() == productId) {
                return itemNode.get("id").asLong();
            }
        }
        throw new IllegalStateException("Added wishlist item not found in response: " + items);
    }

    // ---------------------------------------------------------------------------------------------
    // Direct DB assertions on the wishlist_item table
    // ---------------------------------------------------------------------------------------------

    long userIdOf(String mobile) {
        return jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE mobile = ?", Long.class, mobile);
    }

    int wishlistRowCount(long ownerId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wishlist_item WHERE user_id = ?", Integer.class, ownerId);
    }

    int wishlistRowCount(long ownerId, long productId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wishlist_item WHERE user_id = ? AND product_id = ?",
                Integer.class, ownerId, productId);
    }
}
