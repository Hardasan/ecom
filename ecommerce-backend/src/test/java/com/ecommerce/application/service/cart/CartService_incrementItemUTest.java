package com.ecommerce.application.service.cart;

import com.ecommerce.application.api.dto.cart.CartResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.CartItem;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class CartService_incrementItemUTest extends BaseCartServiceUTest {

    @Test
    void increment_increases_quantity_by_one() {
        Product product = product(PRODUCT_ID, 10);
        CartItem cartItem = item(50L, PRODUCT_ID, VariantType.COLOR, DEFAULT_VARIANT_VALUE, 2, BigDecimal.valueOf(100), null);
        when(cartItemRepository.findByIdAndUserId(50L, USER_ID)).thenReturn(Optional.of(cartItem));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        stubUserItems(cartItem);
        stubProductsForDto(product);

        CartResponseDto response = cartService.incrementItem(USER_ID, 50L);

        assertEquals(3, response.getItems().getFirst().getQuantity());
    }

    @Test
    void increment_beyond_inventory_throws_insufficient_stock() {
        Product product = product(PRODUCT_ID, 2);
        CartItem cartItem = item(50L, PRODUCT_ID, VariantType.COLOR, DEFAULT_VARIANT_VALUE, 2, BigDecimal.valueOf(100), null);
        when(cartItemRepository.findByIdAndUserId(50L, USER_ID)).thenReturn(Optional.of(cartItem));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> cartService.incrementItem(USER_ID, 50L));

        assertEquals(ECOMErrorType.INSUFFICIENT_STOCK, exception.getEcomErrorType());
    }

    @Test
    void increment_unknown_item_throws_cart_item_not_found() {
        when(cartItemRepository.findByIdAndUserId(50L, USER_ID)).thenReturn(Optional.empty());

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> cartService.incrementItem(USER_ID, 50L));

        assertEquals(ECOMErrorType.CART_ITEM_NOT_FOUND, exception.getEcomErrorType());
    }
}
