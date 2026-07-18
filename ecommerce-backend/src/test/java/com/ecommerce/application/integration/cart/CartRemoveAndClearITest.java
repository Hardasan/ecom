package com.ecommerce.application.integration.cart;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartRemoveAndClearITest extends AbstractCartITest {

    @Test
    void remove_item_drops_only_that_line() throws Exception {
        Long productId = createActiveProduct("rm", 10, "#FF0000", "#0000FF");
        long redItemId = addItemAndGetId(userToken, productId, "#FF0000", 1);
        addItemAndGetId(userToken, productId, "#0000FF", 1);

        removeItem(userToken, redItemId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].variantType").value("COLOR"))
                .andExpect(jsonPath("$.items[0].variantValue").value("#0000FF"));
    }

    @Test
    void remove_unknown_item_returns_404() throws Exception {
        removeItem(userToken, 999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CART_ITEM_NOT_FOUND"));
    }

    @Test
    void clear_on_a_user_without_a_cart_is_a_no_op() throws Exception {
        clearCart(userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalQuantity").value(0));
    }

    @Test
    void clear_empties_the_cart() throws Exception {
        Long productId = createActiveProduct("clear", 10, "#FF0000", "#0000FF");
        addItemAndGetId(userToken, productId, "#FF0000", 2);
        addItemAndGetId(userToken, productId, "#0000FF", 1);

        clearCart(userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalQuantity").value(0));
    }
}
