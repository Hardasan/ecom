package com.ecommerce.application.integration.review;

import com.ecommerce.application.api.dto.product.CreateProductRequestDto;
import com.ecommerce.application.api.dto.product.PriceDto;
import com.ecommerce.application.integration.AbstractIntegrationITest;
import com.ecommerce.persistence.entity.enumeration.InventoryStatus;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.ReviewStatus;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class AbstractProductReviewITest extends AbstractIntegrationITest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    String adminToken;
    String userToken;
    String userMobile;
    Long userId;
    Long categoryId;

    @BeforeEach
    void setupReviewFixtures() throws Exception {
        // product (and therefore product_review) is cleared via the FK cascade chain.
        jdbcTemplate.execute("TRUNCATE TABLE category, brand RESTART IDENTITY CASCADE");

        String adminMobile = "09100000000";
        jdbcTemplate.update(
                "INSERT INTO app_user (first_name, last_name, username, mobile, password, role, is_enabled, is_registered) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "Admin", "User", adminMobile, adminMobile,
                passwordEncoder.encode("Admin123!"), "ROLE_ADMIN", true, true);
        adminToken = login(adminMobile, "Admin123!");

        userMobile = newMobile();
        userToken = registerAndLogin(userMobile);
        userId = jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE mobile = ?", Long.class, userMobile);

        categoryId = jdbcTemplate.queryForObject(
                "INSERT INTO category (name) VALUES ('Electronics') RETURNING id", Long.class);
    }

    // ---------------------------------------------------------------------------------------------
    // Product fixture (mirrors AbstractCartITest — reviews only need the product to exist)
    // ---------------------------------------------------------------------------------------------

    Long createActiveProduct(String url, int inventory) throws Exception {
        CreateProductRequestDto req = new CreateProductRequestDto();
        req.setCategoryId(categoryId);
        req.setUrl(url);
        req.setName("Test Product " + url);
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

        MvcResult result = mockMvc.perform(multipart("/api/products")
                        .part(jsonPart("data", req))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("id").asLong();
    }

    org.springframework.mock.web.MockPart jsonPart(String name, Object body) throws Exception {
        org.springframework.mock.web.MockPart part =
                new org.springframework.mock.web.MockPart(name, objectMapper.writeValueAsBytes(body));
        part.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return part;
    }

    /**
     * Simulates a completed purchase by inserting a PAID order + line for the user, so the next
     * review of {@code productId} is stamped {@code verifiedPurchase = true}. The address / zone
     * columns are plain VARCHARs, so valid enum names are used but not otherwise significant.
     */
    void insertPaidOrder(Long buyerUserId, Long productId) {
        Long orderId = jdbcTemplate.queryForObject(
                "INSERT INTO orders (user_id, status, recipient_first_name, recipient_last_name, recipient_mobile, "
                        + "province, city, postal_code, address_line, items_cost, shipping_cost, total_cost, "
                        + "total_weight_gram, shipping_zone) "
                        + "VALUES (?, 'PAID', 'R', 'R', '09120000000', 'TEHRAN', 'Tehran', '1234567890', 'addr', "
                        + "100, 0, 100, 0, 'INTRA_PROVINCE') RETURNING id",
                Long.class, buyerUserId);
        jdbcTemplate.update(
                "INSERT INTO order_item (order_id, product_id, product_name, product_code, variant_type, quantity, "
                        + "unit_price, line_total) VALUES (?, ?, 'P', 'C', 'COLOR', 1, 100, 100)",
                orderId, productId);
    }

    Long reviewRowCount(Long productId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_review WHERE product_id = ?", Long.class, productId);
    }

    // ---------------------------------------------------------------------------------------------
    // Review HTTP helpers
    // ---------------------------------------------------------------------------------------------

    ResultActions postReview(String token, Long productId, Integer rating, String title, String comment)
            throws Exception {
        Map<String, Object> body = new HashMap<>();
        if (rating != null) {
            body.put("rating", rating);
        }
        if (title != null) {
            body.put("title", title);
        }
        if (comment != null) {
            body.put("comment", comment);
        }
        return mockMvc.perform(withAuth(post("/api/products/{productId}/reviews", productId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    long postReviewAndGetId(String token, Long productId, Integer rating, String title, String comment)
            throws Exception {
        MvcResult result = postReview(token, productId, rating, title, comment)
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("id").asLong();
    }

    ResultActions getReviews(String token, Long productId) throws Exception {
        return getReviews(token, productId, null);
    }

    ResultActions getReviews(String token, Long productId, Map<String, String> params) throws Exception {
        MockHttpServletRequestBuilder builder = get("/api/products/{productId}/reviews", productId);
        if (params != null) {
            params.forEach(builder::param);
        }
        return mockMvc.perform(withAuth(builder, token));
    }

    ResultActions getSummary(String token, Long productId) throws Exception {
        return mockMvc.perform(withAuth(get("/api/products/{productId}/reviews/summary", productId), token));
    }

    ResultActions updateReview(String token, Long productId, long reviewId, Integer rating, String title,
            String comment) throws Exception {
        Map<String, Object> body = new HashMap<>();
        if (rating != null) {
            body.put("rating", rating);
        }
        body.put("title", title);
        body.put("comment", comment);
        return mockMvc.perform(withAuth(put("/api/products/{productId}/reviews/{reviewId}", productId, reviewId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    ResultActions deleteReview(String token, Long productId, long reviewId) throws Exception {
        return mockMvc.perform(
                withAuth(delete("/api/products/{productId}/reviews/{reviewId}", productId, reviewId), token));
    }

    ResultActions moderate(String token, Long productId, long reviewId, ReviewStatus status) throws Exception {
        return mockMvc.perform(
                withAuth(patch("/api/products/{productId}/reviews/{reviewId}/status", productId, reviewId), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", status.name()))));
    }
}
