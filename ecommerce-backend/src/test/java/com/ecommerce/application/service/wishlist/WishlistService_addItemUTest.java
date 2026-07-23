package com.ecommerce.application.service.wishlist;

import com.ecommerce.application.api.dto.wishlist.WishlistItemResponseDto;
import com.ecommerce.application.api.dto.wishlist.WishlistResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import org.junit.jupiter.api.Test;

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

        verify(wishlistItemRepository).insertIfAbsent(USER_ID, PRODUCT_ID);
    }

    @Test
    void adding_product_already_on_wishlist_is_idempotent_and_creates_no_row() {
        Product product = activeProduct(PRODUCT_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        // The bookmark already exists, so the conflicting insert creates nothing.
        when(wishlistItemRepository.insertIfAbsent(USER_ID, PRODUCT_ID)).thenReturn(0);
        stubUserItems(item(50L, PRODUCT_ID));
        stubProductsForDto(product);

        WishlistResponseDto response = wishlistService.addItem(USER_ID, addRequest(PRODUCT_ID));

        assertEquals(1, response.getTotalItems());
        assertEquals(PRODUCT_ID, response.getItems().getFirst().getProductId());
    }

    @Test
    void add_never_pre_checks_existence_so_concurrent_adds_cannot_race() {
        // The de-duplication has to happen inside the insert: an exists()-then-save() would let two
        // concurrent adds both see "absent" and collide on uk_wishlist_item_user_product.
        Product product = activeProduct(PRODUCT_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        stubUserItems(item(50L, PRODUCT_ID));
        stubProductsForDto(product);

        wishlistService.addItem(USER_ID, addRequest(PRODUCT_ID));

        verify(wishlistItemRepository, never()).existsByUserIdAndProductId(any(), any());
        verify(wishlistItemRepository, never()).save(any());
    }

    @Test
    void out_of_stock_active_product_can_be_wishlisted_but_flagged_unavailable() {
        Product product = product(PRODUCT_ID, ProductStatus.ACTIVE, 0);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        stubUserItems(item(50L, PRODUCT_ID));
        stubProductsForDto(product);

        WishlistResponseDto response = wishlistService.addItem(USER_ID, addRequest(PRODUCT_ID));

        WishlistItemResponseDto line = response.getItems().getFirst();
        assertFalse(line.getInStock());
        assertFalse(line.getAvailable());
        verify(wishlistItemRepository).insertIfAbsent(USER_ID, PRODUCT_ID);
    }

    @Test
    void unknown_product_throws_product_not_found() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> wishlistService.addItem(USER_ID, addRequest(PRODUCT_ID)));

        assertEquals(ECOMErrorType.PRODUCT_NOT_FOUND, exception.getEcomErrorType());
        verify(wishlistItemRepository, never()).insertIfAbsent(any(), any());
    }

    @Test
    void inactive_product_throws_product_not_available() {
        Product product = product(PRODUCT_ID, ProductStatus.INACTIVE, 10);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> wishlistService.addItem(USER_ID, addRequest(PRODUCT_ID)));

        assertEquals(ECOMErrorType.PRODUCT_NOT_AVAILABLE, exception.getEcomErrorType());
        verify(wishlistItemRepository, never()).insertIfAbsent(any(), any());
    }
}
