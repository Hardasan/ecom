package com.ecommerce.application.integration.cart;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartGetITest extends AbstractCartITest {

    @Test
    void get_without_auth_returns_401() throws Exception {
        getCart(null).andExpect(status().isUnauthorized());
    }

    @Test
    void get_returns_empty_cart_for_new_user() throws Exception {
        getCart(userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalQuantity").value(0))
                .andExpect(jsonPath("$.totalPrice").value(0));
    }

    @Test
    void get_returns_previously_added_items() throws Exception {
        Long productId = createActiveProduct("persist", 10, DEFAULT_VARIANT_VALUE);
        addItem(userToken, productId, DEFAULT_VARIANT_VALUE, 2).andExpect(status().isOk());

        getCart(userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].productName").value("Test Product persist"));
    }

    @Test
    void carts_are_isolated_between_users() throws Exception {
        Long productId = createActiveProduct("isolation", 10, DEFAULT_VARIANT_VALUE);
        addItem(userToken, productId, DEFAULT_VARIANT_VALUE, 1).andExpect(status().isOk());

        String otherUserToken = registerAndLogin(newMobile());
        getCart(otherUserToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }
}
