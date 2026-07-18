package com.ecommerce.application.integration.wishlist;

import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WishlistAddItemITest extends AbstractWishlistITest {

    @Test
    void add_item_persists_row_and_returns_enriched_wishlist() throws Exception {
        Long productId = createActiveProduct("laptop", 10);

        addItem(userToken, productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andExpect(jsonPath("$.items[0].productName").value("Test Product laptop"))
                .andExpect(jsonPath("$.items[0].productCode").value(startsWith(categoryId + "-")))
                .andExpect(jsonPath("$.items[0].inStock").value(true))
                .andExpect(jsonPath("$.items[0].available").value(true))
                .andExpect(jsonPath("$.items[0].addedAt").exists())
                .andExpect(jsonPath("$.totalItems").value(1));

        // DB: exactly one bookmark row for this user + product.
        assertEquals(1, wishlistRowCount(userId));
        assertEquals(1, wishlistRowCount(userId, productId));
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT user_id, product_id, created_at FROM wishlist_item WHERE user_id = ?", userId);
        assertEquals(userId, ((Number) row.get("user_id")).longValue());
        assertEquals(productId, ((Number) row.get("product_id")).longValue());
        assertEquals(true, row.get("created_at") != null);
    }

    @Test
    void add_item_without_auth_returns_401() throws Exception {
        Long productId = createActiveProduct("phone", 10);

        addItem(null, productId).andExpect(status().isUnauthorized());

        assertEquals(0, wishlistRowCount(userId, productId));
    }

    @Test
    void add_unknown_product_returns_404_and_persists_nothing() throws Exception {
        addItem(userToken, 999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));

        assertEquals(0, wishlistRowCount(userId));
    }

    @Test
    void add_inactive_product_returns_409_and_persists_nothing() throws Exception {
        Long productId = createProduct("hidden", 10, ProductStatus.INACTIVE);

        addItem(userToken, productId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_AVAILABLE"));

        assertEquals(0, wishlistRowCount(userId));
    }

    @Test
    void add_out_of_stock_product_succeeds_and_flags_unavailable() throws Exception {
        // Out-of-stock is the classic wishlist use case: save it now, buy it when it is back.
        Long productId = createActiveProduct("soldout", 0);

        addItem(userToken, productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].inStock").value(false))
                .andExpect(jsonPath("$.items[0].available").value(false))
                .andExpect(jsonPath("$.items[0].inventoryStatus").value("OUT_OF_STOCK"));

        assertEquals(1, wishlistRowCount(userId, productId));
    }

    @Test
    void adding_same_product_twice_is_idempotent_single_row() throws Exception {
        Long productId = createActiveProduct("dup", 10);

        addItem(userToken, productId).andExpect(status().isOk());
        addItem(userToken, productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalItems").value(1));

        // The unique (user, product) constraint means the second add did not create a new row.
        assertEquals(1, wishlistRowCount(userId, productId));
    }

    @Test
    void add_missing_product_id_returns_400() throws Exception {
        mockMvc.perform(withAuth(post("/api/wishlist/items"), userToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        assertEquals(0, wishlistRowCount(userId));
    }

    @Test
    void adds_are_isolated_between_users() throws Exception {
        Long productId = createActiveProduct("iso", 10);
        addItem(userToken, productId).andExpect(status().isOk());

        String otherToken = registerAndLogin(newMobile());
        getWishlist(otherToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));

        // Only the first user owns a row; the whole table holds exactly one.
        assertEquals(1, wishlistRowCount(userId));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wishlist_item", Integer.class));
    }
}
