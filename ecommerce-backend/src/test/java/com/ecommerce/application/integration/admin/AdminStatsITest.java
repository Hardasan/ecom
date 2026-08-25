package com.ecommerce.application.integration.admin;

import com.ecommerce.application.api.dto.product.CreateProductRequestDto;
import com.ecommerce.application.api.dto.product.PriceDto;
import com.ecommerce.application.integration.AbstractIntegrationITest;
import com.ecommerce.persistence.entity.enumeration.InventoryStatus;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The admin dashboard summary ({@code GET /api/admin/stats}).
 */
class AdminStatsITest extends AbstractIntegrationITest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private Long categoryId;

    @BeforeEach
    void setUp() throws Exception {
        // Deterministic counts: clear orders + catalog (product / product_review cascade from category).
        jdbcTemplate.execute("TRUNCATE TABLE orders, category, brand RESTART IDENTITY CASCADE");

        String adminMobile = "09100000000";
        jdbcTemplate.update(
                "INSERT INTO app_user (first_name, last_name, username, mobile, password, role, is_enabled, is_registered) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "Admin", "User", adminMobile, adminMobile,
                passwordEncoder.encode("Admin123!"), "ROLE_ADMIN", true, true);
        adminToken = login(adminMobile, "Admin123!");

        userToken = registerAndLogin(newMobile());
        categoryId = jdbcTemplate.queryForObject(
                "INSERT INTO category (name) VALUES ('Electronics') RETURNING id", Long.class);
    }

    @Test
    void stats_reflect_seeded_products_and_a_pending_review() throws Exception {
        Long inStock = createProduct("stats-a", 5);
        createProduct("stats-b", 0);            // out of stock (inventory 0)
        postReview(userToken, inStock, 5);      // a brand-new review is PENDING

        mockMvc.perform(withAuth(get("/api/admin/stats"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(2))
                .andExpect(jsonPath("$.activeProducts").value(2))
                .andExpect(jsonPath("$.outOfStockProducts").value(1))
                .andExpect(jsonPath("$.pendingReviews").value(1))
                .andExpect(jsonPath("$.totalCategories").value(1))
                .andExpect(jsonPath("$.totalOrders").value(0))
                .andExpect(jsonPath("$.totalRevenue").value(0))
                .andExpect(jsonPath("$.ordersByStatus.PAID").value(0));
    }

    @Test
    void stats_is_admin_only() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/stats"), userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());
    }

    private Long createProduct(String url, int inventory) throws Exception {
        CreateProductRequestDto req = new CreateProductRequestDto();
        req.setCategoryId(categoryId);
        req.setUrl(url);
        req.setName("Product " + url);
        req.setStatus(ProductStatus.ACTIVE);
        req.setInventoryStatus(InventoryStatus.IN_STOCK);
        req.setInventoryCount(inventory);
        req.setVariantType(VariantType.COLOR);

        List<PriceDto> prices = new ArrayList<>();
        PriceDto price = new PriceDto();
        price.setPrice(BigDecimal.valueOf(100));
        price.setVariantValue("#FF0000");
        prices.add(price);
        req.setPrices(prices);

        MockPart data = new MockPart("data", objectMapper.writeValueAsBytes(req));
        data.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        MvcResult result = mockMvc.perform(multipart("/api/products")
                        .part(data)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("id").asLong();
    }

    private void postReview(String token, Long productId, int rating) throws Exception {
        mockMvc.perform(withAuth(post("/api/products/{productId}/reviews", productId), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rating", rating))))
                .andExpect(status().isOk());
    }
}
