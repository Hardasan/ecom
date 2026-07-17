package com.ecommerce.application.service.cart;

import com.ecommerce.application.api.dto.cart.CartResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.CartItem;
import com.ecommerce.persistence.entity.Price;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CartService_addItemUTest extends BaseCartServiceUTest {

    @Test
    void adding_to_empty_cart_creates_line_with_price_snapshot_and_totals() {
        Product product = product(PRODUCT_ID, 10, ProductStatus.ACTIVE, VariantType.COLOR, DEFAULT_VARIANT_VALUE,
                BigDecimal.valueOf(100), BigDecimal.valueOf(80));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProductIdAndVariantTypeAndVariantValue(USER_ID, PRODUCT_ID,
                VariantType.COLOR, DEFAULT_VARIANT_VALUE))
                .thenReturn(Optional.empty());
        stubUserItems(item(50L, PRODUCT_ID, VariantType.COLOR, DEFAULT_VARIANT_VALUE, 2,
                BigDecimal.valueOf(100), BigDecimal.valueOf(80)));
        stubProductsForDto(product);

        CartResponseDto response = cartService.addItem(USER_ID,
                addRequest(PRODUCT_ID, VariantType.COLOR, DEFAULT_VARIANT_VALUE, 2));

        var line = response.getItems().getFirst();
        assertEquals(2, line.getQuantity());
        assertEquals(BigDecimal.valueOf(80), line.getEffectivePrice());
        assertEquals(0, line.getLineTotal().compareTo(BigDecimal.valueOf(160)));
        assertEquals(2, response.getTotalQuantity());
        assertEquals(0, response.getTotalPrice().compareTo(BigDecimal.valueOf(160)));

        ArgumentCaptor<CartItem> saved = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(saved.capture());
        assertEquals(USER_ID, saved.getValue().getUserId());
        assertEquals(2, saved.getValue().getQuantity());
        assertEquals(BigDecimal.valueOf(100), saved.getValue().getUnitPrice());
        assertEquals(BigDecimal.valueOf(80), saved.getValue().getDiscountPrice());
    }

    @Test
    void adding_same_product_and_variant_again_merges_into_one_line() {
        Product product = product(PRODUCT_ID, 10);
        CartItem existing = item(50L, PRODUCT_ID, VariantType.COLOR, DEFAULT_VARIANT_VALUE, 1,
                BigDecimal.valueOf(100), null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProductIdAndVariantTypeAndVariantValue(USER_ID, PRODUCT_ID,
                VariantType.COLOR, DEFAULT_VARIANT_VALUE))
                .thenReturn(Optional.of(existing));
        stubUserItems(existing);
        stubProductsForDto(product);

        CartResponseDto response = cartService.addItem(USER_ID,
                addRequest(PRODUCT_ID, VariantType.COLOR, DEFAULT_VARIANT_VALUE, 3));

        assertEquals(1, response.getItems().size());
        assertEquals(4, response.getItems().getFirst().getQuantity());
        verify(cartItemRepository).save(existing);
    }

    @Test
    void adding_different_variant_of_same_product_creates_separate_line() {
        Product product = product(PRODUCT_ID, 10, ProductStatus.ACTIVE, VariantType.COLOR, DEFAULT_VARIANT_VALUE,
                BigDecimal.valueOf(100), null);
        product.getPrices().add(price("#0000FF", BigDecimal.valueOf(120)));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProductIdAndVariantTypeAndVariantValue(USER_ID, PRODUCT_ID,
                VariantType.COLOR, "#0000FF"))
                .thenReturn(Optional.empty());
        stubUserItems(
                item(50L, PRODUCT_ID, VariantType.COLOR, DEFAULT_VARIANT_VALUE, 1, BigDecimal.valueOf(100), null),
                item(51L, PRODUCT_ID, VariantType.COLOR, "#0000FF", 2, BigDecimal.valueOf(120), null));
        stubProductsForDto(product);

        CartResponseDto response = cartService.addItem(USER_ID,
                addRequest(PRODUCT_ID, VariantType.COLOR, "#0000FF", 2));

        assertEquals(2, response.getItems().size());
        assertEquals(3, response.getTotalQuantity());
    }

    @Test
    void unknown_product_throws_product_not_found() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> cartService.addItem(USER_ID, addRequest(PRODUCT_ID, VariantType.COLOR, DEFAULT_VARIANT_VALUE, 1)));

        assertEquals(ECOMErrorType.PRODUCT_NOT_FOUND, exception.getEcomErrorType());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void inactive_product_throws_product_not_available() {
        Product product = product(PRODUCT_ID, 10, ProductStatus.INACTIVE, VariantType.COLOR, DEFAULT_VARIANT_VALUE,
                BigDecimal.valueOf(100), null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> cartService.addItem(USER_ID, addRequest(PRODUCT_ID, VariantType.COLOR, DEFAULT_VARIANT_VALUE, 1)));

        assertEquals(ECOMErrorType.PRODUCT_NOT_AVAILABLE, exception.getEcomErrorType());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void variant_not_offered_by_product_throws_product_variant_not_found() {
        Product product = product(PRODUCT_ID, 10, ProductStatus.ACTIVE, VariantType.COLOR, DEFAULT_VARIANT_VALUE,
                BigDecimal.valueOf(100), null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> cartService.addItem(USER_ID, addRequest(PRODUCT_ID, VariantType.COLOR, "#0000FF", 1)));

        assertEquals(ECOMErrorType.PRODUCT_VARIANT_NOT_FOUND, exception.getEcomErrorType());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void requesting_more_than_inventory_throws_insufficient_stock() {
        Product product = product(PRODUCT_ID, 3);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProductIdAndVariantTypeAndVariantValue(USER_ID, PRODUCT_ID,
                VariantType.COLOR, DEFAULT_VARIANT_VALUE))
                .thenReturn(Optional.empty());

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> cartService.addItem(USER_ID, addRequest(PRODUCT_ID, VariantType.COLOR, DEFAULT_VARIANT_VALUE, 4)));

        assertEquals(ECOMErrorType.INSUFFICIENT_STOCK, exception.getEcomErrorType());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void merged_quantity_exceeding_inventory_throws_insufficient_stock() {
        Product product = product(PRODUCT_ID, 5);
        CartItem existing = item(50L, PRODUCT_ID, VariantType.COLOR, DEFAULT_VARIANT_VALUE, 4,
                BigDecimal.valueOf(100), null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProductIdAndVariantTypeAndVariantValue(USER_ID, PRODUCT_ID,
                VariantType.COLOR, DEFAULT_VARIANT_VALUE))
                .thenReturn(Optional.of(existing));

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> cartService.addItem(USER_ID, addRequest(PRODUCT_ID, VariantType.COLOR, DEFAULT_VARIANT_VALUE, 2)));

        assertEquals(ECOMErrorType.INSUFFICIENT_STOCK, exception.getEcomErrorType());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void adding_variantless_product_creates_line_with_null_variant_fields() {
        Product product = variantlessProduct(PRODUCT_ID, 5, ProductStatus.ACTIVE,
                BigDecimal.valueOf(100), null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndProductIdAndVariantTypeAndVariantValue(USER_ID, PRODUCT_ID, null, null))
                .thenReturn(Optional.empty());
        stubUserItems(variantlessItem(50L, PRODUCT_ID, 2, BigDecimal.valueOf(100), null));
        stubProductsForDto(product);

        CartResponseDto response = cartService.addItem(USER_ID,
                addRequestVariantless(PRODUCT_ID, 2));

        var line = response.getItems().getFirst();
        assertEquals(2, line.getQuantity());
        assertNull(line.getVariantType());
        assertNull(line.getVariantValue());
        assertEquals(0, line.getLineTotal().compareTo(BigDecimal.valueOf(200)));
    }

    @Test
    void adding_variant_to_a_variantless_product_throws_product_variant_not_found() {
        Product product = variantlessProduct(PRODUCT_ID, 5, ProductStatus.ACTIVE,
                BigDecimal.valueOf(100), null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> cartService.addItem(USER_ID,
                        addRequest(PRODUCT_ID, VariantType.COLOR, DEFAULT_VARIANT_VALUE, 1)));

        assertEquals(ECOMErrorType.PRODUCT_VARIANT_NOT_FOUND, exception.getEcomErrorType());
        verify(cartItemRepository, never()).save(any());
    }

    private Price price(String variantValue, BigDecimal value) {
        Price price = new Price();
        price.setVariantValue(variantValue);
        price.setPrice(value);
        return price;
    }
}