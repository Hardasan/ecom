package com.ecommerce.application.integration.cart;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartUpdateQuantityITest extends AbstractCartITest {

    @Test
    void increment_increases_quantity() throws Exception {
        Long productId = createActiveProduct("inc", 10, DEFAULT_VARIANT_VALUE);
        long itemId = addItemAndGetId(userToken, productId, DEFAULT_VARIANT_VALUE, 1);

        incrementItem(userToken, itemId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void increment_beyond_stock_returns_409() throws Exception {
        Long productId = createActiveProduct("inc-limit", 1, DEFAULT_VARIANT_VALUE);
        long itemId = addItemAndGetId(userToken, productId, DEFAULT_VARIANT_VALUE, 1);

        incrementItem(userToken, itemId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void decrement_reduces_quantity() throws Exception {
        Long productId = createActiveProduct("dec", 10, DEFAULT_VARIANT_VALUE);
        long itemId = addItemAndGetId(userToken, productId, DEFAULT_VARIANT_VALUE, 3);

        decrementItem(userToken, itemId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void decrement_to_zero_removes_line() throws Exception {
        Long productId = createActiveProduct("dec-zero", 10, DEFAULT_VARIANT_VALUE);
        long itemId = addItemAndGetId(userToken, productId, DEFAULT_VARIANT_VALUE, 1);

        decrementItem(userToken, itemId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalQuantity").value(0));
    }

    @Test
    void update_sets_absolute_quantity() throws Exception {
        Long productId = createActiveProduct("set-qty", 10, DEFAULT_VARIANT_VALUE);
        long itemId = addItemAndGetId(userToken, productId, DEFAULT_VARIANT_VALUE, 1);

        updateQuantity(userToken, itemId, 5)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5));
    }

    @Test
    void update_above_stock_returns_409() throws Exception {
        Long productId = createActiveProduct("set-limit", 4, DEFAULT_VARIANT_VALUE);
        long itemId = addItemAndGetId(userToken, productId, DEFAULT_VARIANT_VALUE, 1);

        updateQuantity(userToken, itemId, 5)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void update_unknown_item_returns_404() throws Exception {
        updateQuantity(userToken, 999999L, 2)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CART_ITEM_NOT_FOUND"));
    }
}
