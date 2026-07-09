package com.ecommerce.application.service.cart;

import com.ecommerce.application.api.dto.cart.CartResponseDto;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartService_getCartUTest extends BaseCartServiceUTest {

    @Test
    void get_returns_existing_items_with_totals() {
        stubUserItems(item(50L, PRODUCT_ID, VariantType.COLOR, DEFAULT_VARIANT_VALUE, 2, BigDecimal.valueOf(100), null));
        stubProductsForDto(product(PRODUCT_ID, 10));

        CartResponseDto response = cartService.getCart(USER_ID);

        assertEquals(1, response.getItems().size());
        assertEquals(2, response.getTotalQuantity());
        assertEquals(USER_ID, response.getUserId());
        assertEquals("Product " + PRODUCT_ID, response.getItems().getFirst().getProductName());
    }

    @Test
    void get_returns_empty_cart_when_user_has_no_items() {
        stubUserItems();

        CartResponseDto response = cartService.getCart(USER_ID);

        assertTrue(response.getItems().isEmpty());
        assertEquals(0, response.getTotalQuantity());
        assertEquals(0, response.getTotalPrice().compareTo(BigDecimal.ZERO));
        assertEquals(USER_ID, response.getUserId());
    }
}
