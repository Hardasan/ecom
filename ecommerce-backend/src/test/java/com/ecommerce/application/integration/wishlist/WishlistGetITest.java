package com.ecommerce.application.integration.wishlist;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WishlistGetITest extends AbstractWishlistITest {

    @Test
    void get_without_auth_returns_401() throws Exception {
        getWishlist(null).andExpect(status().isUnauthorized());
    }

    @Test
    void get_returns_empty_wishlist_for_new_user() throws Exception {
        getWishlist(userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(0));

        assertEquals(0, wishlistRowCount(userId));
    }

    @Test
    void get_returns_previously_added_items_newest_first() throws Exception {
        Long first = createActiveProduct("first", 10);
        Long second = createActiveProduct("second", 10);
        addItem(userToken, first).andExpect(status().isOk());
        addItem(userToken, second).andExpect(status().isOk());

        getWishlist(userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                // Most recently added appears first.
                .andExpect(jsonPath("$.items[0].productId").value(second))
                .andExpect(jsonPath("$.items[0].productName").value("Test Product second"))
                .andExpect(jsonPath("$.items[1].productId").value(first))
                .andExpect(jsonPath("$.totalItems").value(2));

        assertEquals(2, wishlistRowCount(userId));
    }

    @Test
    void wishlists_are_isolated_between_users() throws Exception {
        Long productId = createActiveProduct("isolation", 10);
        addItem(userToken, productId).andExpect(status().isOk());

        String otherToken = registerAndLogin(newMobile());
        getWishlist(otherToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));

        assertEquals(1, wishlistRowCount(userId));
    }
}
