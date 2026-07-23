package com.ecommerce.application.service.wishlist;

import com.ecommerce.application.api.dto.wishlist.WishlistContainsResponseDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class WishlistService_containsUTest extends BaseWishlistServiceUTest {

    @Test
    void returns_true_when_product_is_on_wishlist() {
        when(wishlistItemRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(true);

        WishlistContainsResponseDto response = wishlistService.contains(USER_ID, PRODUCT_ID);

        assertTrue(response.getInWishlist());
    }

    @Test
    void returns_false_when_product_is_not_on_wishlist() {
        when(wishlistItemRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(false);

        WishlistContainsResponseDto response = wishlistService.contains(USER_ID, PRODUCT_ID);

        assertFalse(response.getInWishlist());
    }
}
