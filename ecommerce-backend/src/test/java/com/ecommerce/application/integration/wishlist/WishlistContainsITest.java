package com.ecommerce.application.integration.wishlist;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WishlistContainsITest extends AbstractWishlistITest {

    @Test
    void contains_without_auth_returns_401() throws Exception {
        contains(null, 1L).andExpect(status().isUnauthorized());
    }

    @Test
    void contains_returns_true_for_a_wishlisted_product() throws Exception {
        Long productId = createActiveProduct("wished", 10);
        addItem(userToken, productId).andExpect(status().isOk());

        contains(userToken, productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inWishlist").value(true));
    }

    @Test
    void contains_returns_false_for_a_product_not_on_the_wishlist() throws Exception {
        Long productId = createActiveProduct("notwished", 10);

        contains(userToken, productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inWishlist").value(false));
    }

    @Test
    void contains_returns_false_for_an_unknown_product() throws Exception {
        // Membership is a pure lookup against the user's rows; a non-existent product is simply absent.
        contains(userToken, 999999L)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inWishlist").value(false));
    }

    @Test
    void contains_is_scoped_to_the_acting_user() throws Exception {
        Long productId = createActiveProduct("peruser", 10);
        addItem(userToken, productId).andExpect(status().isOk());

        String otherToken = registerAndLogin(newMobile());
        contains(otherToken, productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inWishlist").value(false));
    }
}
