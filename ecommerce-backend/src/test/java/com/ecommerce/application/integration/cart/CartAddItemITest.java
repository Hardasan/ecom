package com.ecommerce.application.integration.cart;

import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartAddItemITest extends AbstractCartITest {

    @Test
    void add_item_returns_cart_with_line_and_totals() throws Exception {
        Long productId = createActiveProduct("laptop", 10, DEFAULT_VARIANT_VALUE);

        addItem(userToken, productId, DEFAULT_VARIANT_VALUE, 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andExpect(jsonPath("$.items[0].variantType").value("COLOR"))
                .andExpect(jsonPath("$.items[0].variantValue").value(DEFAULT_VARIANT_VALUE))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(100.0))
                .andExpect(jsonPath("$.items[0].lineTotal").value(200.0))
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andExpect(jsonPath("$.totalPrice").value(200.0));
    }

    @Test
    void add_item_without_auth_returns_401() throws Exception {
        Long productId = createActiveProduct("phone", 10, DEFAULT_VARIANT_VALUE);

        addItem(null, productId, DEFAULT_VARIANT_VALUE, 1)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void add_unknown_product_returns_404() throws Exception {
        addItem(userToken, 999999L, DEFAULT_VARIANT_VALUE, 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void add_variant_not_offered_returns_404() throws Exception {
        Long productId = createActiveProduct("watch", 10, DEFAULT_VARIANT_VALUE);

        addItem(userToken, productId, "#0000FF", 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_VARIANT_NOT_FOUND"));
    }

    @Test
    void add_inactive_product_returns_409() throws Exception {
        Long productId = createProduct("hidden", 10, ProductStatus.INACTIVE, DEFAULT_VARIANT_VALUE);

        addItem(userToken, productId, DEFAULT_VARIANT_VALUE, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_AVAILABLE"));
    }

    @Test
    void add_more_than_stock_returns_409() throws Exception {
        Long productId = createActiveProduct("rare", 3, DEFAULT_VARIANT_VALUE);

        addItem(userToken, productId, DEFAULT_VARIANT_VALUE, 4)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void add_invalid_quantity_returns_400() throws Exception {
        Long productId = createActiveProduct("gadget", 10, DEFAULT_VARIANT_VALUE);

        addItem(userToken, productId, DEFAULT_VARIANT_VALUE, 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void adding_same_product_and_variant_twice_merges_quantities() throws Exception {
        Long productId = createActiveProduct("merge", 10, DEFAULT_VARIANT_VALUE);

        addItem(userToken, productId, DEFAULT_VARIANT_VALUE, 2).andExpect(status().isOk());
        addItem(userToken, productId, DEFAULT_VARIANT_VALUE, 3)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.totalQuantity").value(5));
    }

    @Test
    void adding_different_variants_creates_separate_lines() throws Exception {
        Long productId = createActiveProduct("multi", 10, "#FF0000", "#0000FF");

        addItem(userToken, productId, "#FF0000", 1).andExpect(status().isOk());
        addItem(userToken, productId, "#0000FF", 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalQuantity").value(2));
    }
}
