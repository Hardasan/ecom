package com.ecommerce.application.integration.checkout;

import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.Province;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckoutITest extends AbstractCheckoutITest {

    @Test
    void checkout_creates_pending_order_decrements_inventory_and_clears_cart() throws Exception {
        Long productId = createActiveProduct("laptop", 10, 500);
        addToCart(userToken, productId, VariantType.COLOR, 2);
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        MvcResult result = checkout(userToken, addressId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].lineTotal").value(200.0))
                .andExpect(jsonPath("$.itemsCost").value(200.0))
                .andExpect(jsonPath("$.totalWeightGram").value(1000))
                .andExpect(jsonPath("$.shippingZone").value("INTRA_PROVINCE"))
                .andExpect(jsonPath("$.shippingCost").value(183000.0))
                .andExpect(jsonPath("$.totalCost").value(183200.0))
                .andExpect(jsonPath("$.province").value("TEHRAN"))
                .andExpect(jsonPath("$.recipientFirstName").value("Ali"))
                .andReturn();

        long orderId = json(result).get("id").asLong();

        // cart is emptied at checkout
        mockMvc.perform(withAuth(get("/api/cart"), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));

        // stock IS decremented at checkout (reserved for the pending order)
        assertEquals(8, inventoryOf(productId));

        // order is retrievable
        mockMvc.perform(withAuth(get("/api/orders/{id}", orderId), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));
        listOrders(userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void checkout_to_adjacent_province_uses_adjacent_tariff() throws Exception {
        Long productId = createActiveProduct("phone", 10, 300);
        addToCart(userToken, productId, VariantType.COLOR, 1);
        long addressId = createAddressAndGetId(userToken, Province.ALBORZ);

        checkout(userToken, addressId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shippingZone").value("ADJACENT_PROVINCE"))
                .andExpect(jsonPath("$.shippingCost").value(260000.0));
    }

    @Test
    void checkout_with_heavy_cart_uses_over_threshold_tariff() throws Exception {
        Long productId = createActiveProduct("anvil", 10, 600);
        addToCart(userToken, productId, VariantType.COLOR, 2); // 1200g > 1000g threshold
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        checkout(userToken, addressId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWeightGram").value(1200))
                .andExpect(jsonPath("$.shippingZone").value("INTRA_PROVINCE"))
                .andExpect(jsonPath("$.shippingCost").value(570000.0));
    }

    @Test
    void checkout_to_non_adjacent_province_uses_non_adjacent_tariff() throws Exception {
        Long productId = createActiveProduct("book", 10, 300);
        addToCart(userToken, productId, VariantType.COLOR, 1);
        long addressId = createAddressAndGetId(userToken, Province.FARS);

        checkout(userToken, addressId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shippingZone").value("NON_ADJACENT_PROVINCE"))
                .andExpect(jsonPath("$.shippingCost").value(282200.0));
    }

    @Test
    void checkout_with_multiple_different_products_creates_separate_order_lines() throws Exception {
        Long product1 = createActiveProduct("laptop", 10, 500);
        Long product2 = createActiveProduct("mouse", 20, 100);
        addToCart(userToken, product1, VariantType.COLOR, 1);
        addToCart(userToken, product2, VariantType.COLOR, 2);
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        checkout(userToken, addressId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.itemsCost").value(300.0))  // 100*1 + 100*2
                .andExpect(jsonPath("$.totalWeightGram").value(700)); // 500*1 + 100*2
    }

    @Test
    void same_product_and_variant_in_cart_merges_into_single_order_line() throws Exception {
        Long productId = createActiveProduct("laptop", 10, 500);
        addToCart(userToken, productId, VariantType.COLOR, 1);
        addToCart(userToken, productId, VariantType.COLOR, 2); // same product+variant -> merge
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        checkout(userToken, addressId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].lineTotal").value(300.0));
    }

    @Test
    void same_product_different_variants_creates_separate_order_lines() throws Exception {
        Long productId = createProduct("phone", 10, 300, ProductStatus.ACTIVE, VariantType.COLOR, VariantType.SIZE);
        addToCart(userToken, productId, VariantType.COLOR, 1);
        addToCart(userToken, productId, VariantType.SIZE, 2);
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        checkout(userToken, addressId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    void order_response_includes_reserved_until() throws Exception {
        Long productId = createActiveProduct("laptop", 10, 500);
        addToCart(userToken, productId, VariantType.COLOR, 1);
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        MvcResult result = checkout(userToken, addressId)
                .andExpect(status().isOk())
                .andReturn();

        assertNotNull(json(result).get("reservedUntil").asText());
    }

    @Test
    void order_snapshot_captures_full_address() throws Exception {
        Long productId = createActiveProduct("laptop", 10, 500);
        addToCart(userToken, productId, VariantType.COLOR, 1);
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        checkout(userToken, addressId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientFirstName").value("Ali"))
                .andExpect(jsonPath("$.recipientLastName").value("Rezaei"))
                .andExpect(jsonPath("$.recipientMobile").value("09120000000"))
                .andExpect(jsonPath("$.province").value("TEHRAN"))
                .andExpect(jsonPath("$.city").value("City"))
                .andExpect(jsonPath("$.postalCode").value("1234567890"))
                .andExpect(jsonPath("$.addressLine").value("Some St, No 10"));
    }

    @Test
    void checkout_uses_catalog_price_not_cart_snapshot() throws Exception {
        Long productId = createActiveProduct("laptop", 10, 500);
        addToCart(userToken, productId, VariantType.COLOR, 1);
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        // Simulate a catalog price change after the item was added to cart
        jdbcTemplate.update("UPDATE product_price SET price = ? WHERE product_id = ? AND variant_type = 'COLOR'",
                BigDecimal.valueOf(150), productId);

        checkout(userToken, addressId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].lineTotal").value(150.0))
                .andExpect(jsonPath("$.itemsCost").value(150.0));
    }

    @Test
    void discount_price_wins_over_unit_price() throws Exception {
        Long productId = createProductWithPrices("discount-laptop", 10, 500,
                BigDecimal.valueOf(100), BigDecimal.valueOf(70));
        addToCart(userToken, productId, VariantType.COLOR, 2);
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        checkout(userToken, addressId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemsCost").value(140.0));
    }

    @Test
    void checkout_with_empty_cart_returns_409() throws Exception {
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        checkout(userToken, addressId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMPTY_CART"));
    }

    @Test
    void checkout_with_unknown_address_returns_404() throws Exception {
        Long productId = createActiveProduct("watch", 10, 200);
        addToCart(userToken, productId, VariantType.COLOR, 1);

        checkout(userToken, 999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ADDRESS_NOT_FOUND"));
    }

    @Test
    void checkout_with_another_users_address_returns_404() throws Exception {
        Long productId = createActiveProduct("tablet", 10, 200);
        addToCart(userToken, productId, VariantType.COLOR, 1);

        String otherToken = registerAndLogin(newMobile());
        long otherAddressId = createAddressAndGetId(otherToken, Province.TEHRAN);

        checkout(userToken, otherAddressId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ADDRESS_NOT_FOUND"));
    }

    @Test
    void checkout_with_inactive_product_in_cart_returns_409() throws Exception {
        Long productId = createActiveProduct("expiring-item", 10, 500);
        addToCart(userToken, productId, VariantType.COLOR, 1);
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        // Product becomes inactive between add-to-cart and checkout
        jdbcTemplate.update("UPDATE product SET status = 'INACTIVE' WHERE id = ?", productId);

        checkout(userToken, addressId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_AVAILABLE"));
    }

    @Test
    void checkout_when_product_no_longer_has_the_variant_returns_404() throws Exception {
        Long productId = createProduct("dynamic-item", 10, 300, ProductStatus.ACTIVE, VariantType.COLOR);
        addToCart(userToken, productId, VariantType.COLOR, 1);
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        // Variant removed from catalog between add-to-cart and checkout
        jdbcTemplate.update("DELETE FROM product_price WHERE product_id = ? AND variant_type = 'COLOR'", productId);

        checkout(userToken, addressId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_VARIANT_NOT_FOUND"));
    }

    @Test
    void checkout_without_auth_returns_401() throws Exception {
        mockMvc.perform(post("/api/checkout")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void user_cannot_read_another_users_order() throws Exception {
        Long productId = createActiveProduct("camera", 10, 200);
        addToCart(userToken, productId, VariantType.COLOR, 1);
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);
        MvcResult result = checkout(userToken, addressId).andExpect(status().isOk()).andReturn();
        long orderId = json(result).get("id").asLong();

        String otherToken = registerAndLogin(newMobile());
        mockMvc.perform(withAuth(get("/api/orders/{id}", orderId), otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"));
    }
}
