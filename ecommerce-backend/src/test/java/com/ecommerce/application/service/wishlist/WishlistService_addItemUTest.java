package com.ecommerce.application.service.wishlist;

import com.ecommerce.application.api.dto.wishlist.WishlistItemResponseDto;
import com.ecommerce.application.api.dto.wishlist.WishlistResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.WishlistItem;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WishlistService_addItemUTest extends BaseWishlistServiceUTest {

    @Test
    void adding_to_empty_wishlist_saves_bookmark_and_returns_enriched_item() {
        Product product = activeProduct(PRODUCT_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(wishlistItemRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(false);
        stubUserItems(item(50L, PRODUCT_ID));
        stubProductsForDto(product);

        WishlistResponseDto response = wishlistService.addItem(USER_ID, addRequest(PRODUCT_ID));

        assertEquals(1, response.getTotalItems());
        WishlistItemResponseDto line = response.getItems().getFirst();
        assertEquals(PRODUCT_ID, line.getProductId());
        assertEquals("Product " + PRODUCT_ID, line.getProductName());
        assertEquals(PRODUCT_ID + "-1", line.getProductCode());
        assertTrue(line.getInStock());
        assertTrue(line.getAvailable());

        ArgumentCaptor<WishlistItem> saved = ArgumentCaptor.forClass(WishlistItem.class);
        verify(wishlistItemRepository).save(saved.capture());
        assertEquals(USER_ID, saved.getValue().getUserId());
        assertEquals(PRODUCT_ID, saved.getValue().getProductId());
    }

    @Test
    void adding_product_already_on_wishlist_is_idempotent_and_saves_nothing() {
        Product product = activeProduct(PRODUCT_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(wishlistItemRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(true);
        stubUserItems(item(50L, PRODUCT_ID));
        stubProductsForDto(product);

        WishlistResponseDto response = wishlistService.addItem(USER_ID, addRequest(PRODUCT_ID));

        assertEquals(1, response.getTotalItems());
        verify(wishlistItemRepository, never()).save(any());
    }

    @Test
    void out_of_stock_active_product_can_be_wishlisted_but_flagged_unavailable() {
        Product product = product(PRODUCT_ID, ProductStatus.ACTIVE, 0);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(wishlistItemRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(false);
        stubUserItems(item(50L, PRODUCT_ID));
        stubProductsForDto(product);

        WishlistResponseDto response = wishlistService.addItem(USER_ID, addRequest(PRODUCT_ID));

        WishlistItemResponseDto line = response.getItems().getFirst();
        assertFalse(line.getInStock());
        assertFalse(line.getAvailable());
        verify(wishlistItemRepository).save(any(WishlistItem.class));
    }

    @Test
    void unknown_product_throws_product_not_found() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> wishlistService.addItem(USER_ID, addRequest(PRODUCT_ID)));

        assertEquals(ECOMErrorType.PRODUCT_NOT_FOUND, exception.getEcomErrorType());
        verify(wishlistItemRepository, never()).save(any());
    }

    @Test
    void inactive_product_throws_product_not_available() {
        Product product = product(PRODUCT_ID, ProductStatus.INACTIVE, 10);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> wishlistService.addItem(USER_ID, addRequest(PRODUCT_ID)));

        assertEquals(ECOMErrorType.PRODUCT_NOT_AVAILABLE, exception.getEcomErrorType());
        verify(wishlistItemRepository, never()).save(any());
    }
}
