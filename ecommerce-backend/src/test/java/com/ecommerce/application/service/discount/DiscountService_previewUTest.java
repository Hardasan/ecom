package com.ecommerce.application.service.discount;

import com.ecommerce.application.api.dto.discount.DiscountPreviewResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.CartItem;
import com.ecommerce.persistence.entity.Discount;
import com.ecommerce.persistence.entity.Price;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.enumeration.DiscountScope;
import com.ecommerce.persistence.entity.enumeration.DiscountType;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DiscountService_previewUTest extends BaseDiscountServiceUTest {

    private static final String CODE = "SAVE20";

    private static void assertAmount(long expected, BigDecimal actual) {
        assertEquals(0, BigDecimal.valueOf(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }

    @Test
    void unknown_code_throws_code_invalid() {
        when(discountRepository.findByCodeIgnoreCase(CODE)).thenReturn(java.util.Optional.empty());

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.preview(USER_ID, "save20"));
        assertEquals(ECOMErrorType.DISCOUNT_CODE_INVALID, ex.getEcomErrorType());
    }

    @Test
    void expired_code_throws() {
        Discount discount = usableDiscount();
        discount.setExpiresAt(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)));
        when(discountRepository.findByCodeIgnoreCase(CODE)).thenReturn(java.util.Optional.of(discount));

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.preview(USER_ID, CODE));
        assertEquals(ECOMErrorType.DISCOUNT_EXPIRED, ex.getEcomErrorType());
    }

    @Test
    void global_usage_limit_reached_throws() {
        Discount discount = usableDiscount();
        discount.setUsageLimit(5);
        discount.setUsageCount(5);
        when(discountRepository.findByCodeIgnoreCase(CODE)).thenReturn(java.util.Optional.of(discount));

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.preview(USER_ID, CODE));
        assertEquals(ECOMErrorType.DISCOUNT_USAGE_LIMIT_REACHED, ex.getEcomErrorType());
    }

    @Test
    void per_user_limit_reached_throws() {
        Discount discount = usableDiscount();
        discount.setPerUserLimit(1);
        when(discountRepository.findByCodeIgnoreCase(CODE)).thenReturn(java.util.Optional.of(discount));
        when(orderRepository.countActiveByDiscountAndUser(DISCOUNT_ID, USER_ID)).thenReturn(1L);

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.preview(USER_ID, CODE));
        assertEquals(ECOMErrorType.DISCOUNT_USAGE_LIMIT_REACHED, ex.getEcomErrorType());
    }

    @Test
    void minimum_not_met_throws() {
        Discount discount = usableDiscount();
        discount.setMinimumCartAmount(BigDecimal.valueOf(500_000));
        when(discountRepository.findByCodeIgnoreCase(CODE)).thenReturn(java.util.Optional.of(discount));
        when(cartItemRepository.findByUserId(USER_ID)).thenReturn(List.of(cartItem(1L, "V", 2)));
        when(productRepository.findAllById(any())).thenReturn(List.of(product(1L, "V", BigDecimal.valueOf(100_000))));

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.preview(USER_ID, CODE));
        assertEquals(ECOMErrorType.DISCOUNT_MINIMUM_NOT_MET, ex.getEcomErrorType());
    }

    @Test
    void empty_cart_throws_not_applicable() {
        Discount discount = usableDiscount();
        when(discountRepository.findByCodeIgnoreCase(CODE)).thenReturn(java.util.Optional.of(discount));
        when(cartItemRepository.findByUserId(USER_ID)).thenReturn(List.of());

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.preview(USER_ID, CODE));
        assertEquals(ECOMErrorType.DISCOUNT_NOT_APPLICABLE, ex.getEcomErrorType());
    }

    @Test
    void success_returns_computed_amounts() {
        Discount discount = usableDiscount(); // 20% off everything
        when(discountRepository.findByCodeIgnoreCase(CODE)).thenReturn(java.util.Optional.of(discount));
        when(cartItemRepository.findByUserId(USER_ID)).thenReturn(List.of(cartItem(1L, "V", 2)));
        when(productRepository.findAllById(any())).thenReturn(List.of(product(1L, "V", BigDecimal.valueOf(100_000))));

        DiscountPreviewResponseDto response = service.preview(USER_ID, "save20");

        assertEquals("SAVE20", response.getCode());
        assertEquals(DiscountType.PERCENTAGE, response.getType());
        assertAmount(200_000, response.getItemsCost());
        assertAmount(200_000, response.getEligibleSubtotal());
        assertAmount(40_000, response.getDiscountAmount());   // 20% of 200,000
        assertAmount(160_000, response.getNewItemsCost());
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    void success_uses_product_discount_price_when_present() {
        Discount discount = discount(DISCOUNT_ID, CODE, DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(10_000), DiscountScope.ALL);
        when(discountRepository.findByCodeIgnoreCase(CODE)).thenReturn(java.util.Optional.of(discount));
        Product product = product(1L, "V", BigDecimal.valueOf(100_000));
        product.getPrices().get(0).setDiscountPrice(BigDecimal.valueOf(80_000)); // sale price wins
        when(cartItemRepository.findByUserId(USER_ID)).thenReturn(List.of(cartItem(1L, "V", 1)));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));

        DiscountPreviewResponseDto response = service.preview(USER_ID, CODE);

        assertAmount(80_000, response.getItemsCost());        // sale price, not list price
        assertAmount(10_000, response.getDiscountAmount());
        assertAmount(70_000, response.getNewItemsCost());
    }

    private CartItem cartItem(Long productId, String variantValue, int quantity) {
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setVariantType(VariantType.COLOR);
        item.setVariantValue(variantValue);
        item.setQuantity(quantity);
        return item;
    }

    private Product product(Long id, String variantValue, BigDecimal price) {
        Product product = new Product();
        product.setId(id);
        product.setCategoryId(5L);
        Price priceRow = new Price();
        priceRow.setVariantValue(variantValue);
        priceRow.setPrice(price);
        product.setPrices(new ArrayList<>(List.of(priceRow)));
        return product;
    }
}
