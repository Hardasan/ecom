package com.ecommerce.application.integration.wishlist;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WishlistRemoveAndClearITest extends AbstractWishlistITest {

    @Test
    void remove_by_item_id_deletes_the_row() throws Exception {
        Long productId = createActiveProduct("rm", 10);
        long itemId = addItemAndGetId(userToken, productId);
        assertEquals(1, wishlistRowCount(userId, productId));

        removeItem(userToken, itemId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(0));

        assertEquals(0, wishlistRowCount(userId));
    }

    @Test
    void remove_drops_only_the_targeted_row() throws Exception {
        Long keep = createActiveProduct("keep", 10);
        Long drop = createActiveProduct("drop", 10);
        addItemAndGetId(userToken, keep);
        long dropItemId = addItemAndGetId(userToken, drop);

        removeItem(userToken, dropItemId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(keep));

        assertEquals(1, wishlistRowCount(userId, keep));
        assertEquals(0, wishlistRowCount(userId, drop));
    }

    @Test
    void remove_unknown_item_returns_404() throws Exception {
        removeItem(userToken, 999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("WISHLIST_ITEM_NOT_FOUND"));
    }

    @Test
    void remove_by_product_id_deletes_the_row() throws Exception {
        Long productId = createActiveProduct("rmbyproduct", 10);
        addItemAndGetId(userToken, productId);
        assertEquals(1, wishlistRowCount(userId, productId));

        removeByProduct(userToken, productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));

        assertEquals(0, wishlistRowCount(userId, productId));
    }

    @Test
    void remove_by_product_not_on_wishlist_returns_404() throws Exception {
        Long productId = createActiveProduct("absent", 10);

        removeByProduct(userToken, productId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("WISHLIST_ITEM_NOT_FOUND"));
    }

    @Test
    void cannot_remove_another_users_item() throws Exception {
        Long productId = createActiveProduct("mine", 10);
        long itemId = addItemAndGetId(userToken, productId);

        String otherToken = registerAndLogin(newMobile());
        removeItem(otherToken, itemId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("WISHLIST_ITEM_NOT_FOUND"));

        // The owner's row is untouched.
        assertEquals(1, wishlistRowCount(userId, productId));
    }

    @Test
    void clear_deletes_all_rows() throws Exception {
        addItemAndGetId(userToken, createActiveProduct("c1", 10));
        addItemAndGetId(userToken, createActiveProduct("c2", 10));
        assertEquals(2, wishlistRowCount(userId));

        clearWishlist(userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(0));

        assertEquals(0, wishlistRowCount(userId));
    }

    @Test
    void clear_on_empty_wishlist_is_a_no_op() throws Exception {
        clearWishlist(userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(0));

        assertEquals(0, wishlistRowCount(userId));
    }

    @Test
    void clear_only_affects_the_acting_user() throws Exception {
        Long productId = createActiveProduct("shared", 10);
        addItemAndGetId(userToken, productId);

        String otherMobile = newMobile();
        String otherToken = registerAndLogin(otherMobile);
        long otherId = userIdOf(otherMobile);
        addItemAndGetId(otherToken, productId);

        clearWishlist(otherToken).andExpect(status().isOk());

        assertEquals(0, wishlistRowCount(otherId));
        // The first user's bookmark survives the other user's clear.
        assertEquals(1, wishlistRowCount(userId, productId));
    }
}
