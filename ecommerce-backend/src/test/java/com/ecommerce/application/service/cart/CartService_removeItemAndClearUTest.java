package com.ecommerce.application.service.cart;

import com.ecommerce.application.api.dto.cart.CartResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.CartItem;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartService_removeItemAndClearUTest extends BaseCartServiceUTest {

    @Test
    void remove_drops_the_targeted_line_only() {
        CartItem colorItem = item(50L, PRODUCT_ID, VariantType.COLOR, DEFAULT_VARIANT_VALUE, 1, BigDecimal.valueOf(100), null);
        CartItem sizeItem = item(51L, PRODUCT_ID, VariantType.SIZE, "M", 1, BigDecimal.valueOf(100), null);
        when(cartItemRepository.findByIdAndUserId(50L, USER_ID)).thenReturn(Optional.of(colorItem));
        stubUserItems(sizeItem);
        stubProductsForDto(product(PRODUCT_ID, 10));

        CartResponseDto response = cartService.removeItem(USER_ID, 50L);

        assertEquals(1, response.getItems().size());
        assertEquals(51L, response.getItems().getFirst().getId());
        verify(cartItemRepository).delete(colorItem);
    }

    @Test
    void remove_unknown_item_throws_cart_item_not_found() {
        when(cartItemRepository.findByIdAndUserId(50L, USER_ID)).thenReturn(Optional.empty());

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> cartService.removeItem(USER_ID, 50L));

        assertEquals(ECOMErrorType.CART_ITEM_NOT_FOUND, exception.getEcomErrorType());
    }

    @Test
    void clear_empties_all_lines() {
        CartResponseDto response = cartService.clearCart(USER_ID);

        assertTrue(response.getItems().isEmpty());
        assertEquals(0, response.getTotalQuantity());
        verify(cartItemRepository).deleteByUserId(USER_ID);
    }

    @Test
    void clear_on_no_cart_is_a_no_op_returning_empty_cart() {
        CartResponseDto response = cartService.clearCart(USER_ID);

        assertTrue(response.getItems().isEmpty());
        assertEquals(0, response.getTotalQuantity());
        verify(cartItemRepository).deleteByUserId(USER_ID);
    }
}
