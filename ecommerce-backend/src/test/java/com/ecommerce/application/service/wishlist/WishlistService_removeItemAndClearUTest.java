package com.ecommerce.application.service.wishlist;

import com.ecommerce.application.api.dto.wishlist.WishlistResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.WishlistItem;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WishlistService_removeItemAndClearUTest extends BaseWishlistServiceUTest {

    @Test
    void remove_by_item_id_deletes_that_bookmark() {
        WishlistItem target = item(50L, PRODUCT_ID);
        when(wishlistItemRepository.findByIdAndUserId(50L, USER_ID)).thenReturn(Optional.of(target));
        stubUserItems();

        WishlistResponseDto response = wishlistService.removeItem(USER_ID, 50L);

        assertTrue(response.getItems().isEmpty());
        assertEquals(0, response.getTotalItems());
        verify(wishlistItemRepository).delete(target);
    }

    @Test
    void remove_unknown_item_throws_wishlist_item_not_found() {
        when(wishlistItemRepository.findByIdAndUserId(50L, USER_ID)).thenReturn(Optional.empty());

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> wishlistService.removeItem(USER_ID, 50L));

        assertEquals(ECOMErrorType.WISHLIST_ITEM_NOT_FOUND, exception.getEcomErrorType());
        verify(wishlistItemRepository, never()).delete(any());
    }

    @Test
    void remove_by_product_deletes_the_bookmark() {
        WishlistItem target = item(50L, PRODUCT_ID);
        when(wishlistItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.of(target));
        stubUserItems();

        WishlistResponseDto response = wishlistService.removeByProduct(USER_ID, PRODUCT_ID);

        assertTrue(response.getItems().isEmpty());
        verify(wishlistItemRepository).delete(target);
    }

    @Test
    void remove_by_product_not_on_wishlist_throws_wishlist_item_not_found() {
        when(wishlistItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.empty());

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> wishlistService.removeByProduct(USER_ID, PRODUCT_ID));

        assertEquals(ECOMErrorType.WISHLIST_ITEM_NOT_FOUND, exception.getEcomErrorType());
        verify(wishlistItemRepository, never()).delete(any());
    }

    @Test
    void clear_empties_all_bookmarks() {
        WishlistResponseDto response = wishlistService.clear(USER_ID);

        assertTrue(response.getItems().isEmpty());
        assertEquals(0, response.getTotalItems());
        verify(wishlistItemRepository).deleteByUserId(USER_ID);
    }

    @Test
    void clear_on_empty_wishlist_is_a_no_op_returning_empty_wishlist() {
        WishlistResponseDto response = wishlistService.clear(USER_ID);

        assertTrue(response.getItems().isEmpty());
        assertEquals(0, response.getTotalItems());
        verify(wishlistItemRepository).deleteByUserId(USER_ID);
    }
}
