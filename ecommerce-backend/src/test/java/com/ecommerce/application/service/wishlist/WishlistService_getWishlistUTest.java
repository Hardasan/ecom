package com.ecommerce.application.service.wishlist;

import com.ecommerce.application.api.dto.wishlist.WishlistItemResponseDto;
import com.ecommerce.application.api.dto.wishlist.WishlistResponseDto;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.enumeration.InventoryStatus;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WishlistService_getWishlistUTest extends BaseWishlistServiceUTest {

    @Test
    void empty_wishlist_returns_no_items() {
        stubUserItems();

        WishlistResponseDto response = wishlistService.getWishlist(USER_ID);

        assertEquals(USER_ID, response.getUserId());
        assertTrue(response.getItems().isEmpty());
        assertEquals(0, response.getTotalItems());
        verify(productRepository, never()).findAllById(any());
    }

    @Test
    void returns_items_enriched_with_product_details() {
        Product product = activeProduct(PRODUCT_ID);
        stubUserItems(item(50L, PRODUCT_ID));
        stubProductsForDto(product);

        WishlistResponseDto response = wishlistService.getWishlist(USER_ID);

        assertEquals(1, response.getTotalItems());
        WishlistItemResponseDto line = response.getItems().getFirst();
        assertEquals(50L, line.getId());
        assertEquals(PRODUCT_ID, line.getProductId());
        assertEquals("Product " + PRODUCT_ID, line.getProductName());
        assertEquals("محصول " + PRODUCT_ID, line.getProductLocalName());
        assertEquals(PRODUCT_ID + "-1", line.getProductCode());
        assertEquals("product-" + PRODUCT_ID, line.getProductUrl());
        assertNotNull(line.getMainImage());
        assertEquals("alt " + PRODUCT_ID, line.getMainImage().getAltText());
        assertEquals(1, line.getPrices().size());
        assertEquals(0, line.getPrices().getFirst().getPrice().compareTo(BigDecimal.valueOf(100)));
        assertEquals(ProductStatus.ACTIVE, line.getStatus());
        assertEquals(InventoryStatus.IN_STOCK, line.getInventoryStatus());
        assertTrue(line.getInStock());
        assertTrue(line.getAvailable());
        assertNotNull(line.getAddedAt());
    }

    @Test
    void out_of_stock_item_is_marked_unavailable() {
        Product product = product(PRODUCT_ID, ProductStatus.ACTIVE, 0);
        stubUserItems(item(50L, PRODUCT_ID));
        stubProductsForDto(product);

        WishlistResponseDto response = wishlistService.getWishlist(USER_ID);

        WishlistItemResponseDto line = response.getItems().getFirst();
        assertEquals(InventoryStatus.OUT_OF_STOCK, line.getInventoryStatus());
        assertFalse(line.getInStock());
        assertFalse(line.getAvailable());
    }
}
