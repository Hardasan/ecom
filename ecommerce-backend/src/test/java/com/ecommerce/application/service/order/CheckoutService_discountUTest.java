package com.ecommerce.application.service.order;

import com.ecommerce.application.api.dto.order.CheckoutRequestDto;
import com.ecommerce.application.api.dto.order.OrderResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.application.service.discount.AppliedDiscount;
import com.ecommerce.application.service.shipping.ShippingResult;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.Province;
import com.ecommerce.persistence.entity.enumeration.ShippingZone;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CheckoutService_discountUTest extends BaseCheckoutServiceUTest {

    private static final BigDecimal SHIPPING_COST = BigDecimal.valueOf(183000);

    private CheckoutRequestDto request(String code) {
        CheckoutRequestDto dto = new CheckoutRequestDto();
        dto.setAddressId(ADDRESS_ID);
        dto.setDiscountCode(code);
        return dto;
    }

    private void stubCartAndProduct() {
        when(userAddressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address()));
        when(cartItemRepository.findByUserId(USER_ID)).thenReturn(List.of(cartItem(2))); // itemsCost = 100 * 2
        Product product = product(10, 500, ProductStatus.ACTIVE);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
    }

    @Test
    void checkout_with_code_applies_discount_snapshots_it_and_reduces_total() {
        stubCartAndProduct();
        when(shippingCalculator.calculate(eq(Province.TEHRAN), anyInt()))
                .thenReturn(new ShippingResult(ShippingZone.INTRA_PROVINCE, SHIPPING_COST));
        when(discountService.redeemForOrder(eq("SAVE20"), eq(USER_ID), anyList()))
                .thenReturn(new AppliedDiscount(55L, "SAVE20", BigDecimal.valueOf(50)));

        OrderResponseDto response = checkoutService.checkout(USER_ID, request("SAVE20"));

        assertEquals("SAVE20", response.getDiscountCode());
        assertEquals(0, BigDecimal.valueOf(50).compareTo(response.getDiscountAmount()));
        // total = itemsCost 200 - discount 50 + shipping
        assertEquals(0, SHIPPING_COST.add(BigDecimal.valueOf(150)).compareTo(response.getTotalCost()));
        verify(discountService).redeemForOrder(eq("SAVE20"), eq(USER_ID), anyList());
    }

    @Test
    void checkout_without_code_leaves_zero_discount_and_skips_discount_service() {
        stubCartAndProduct();
        when(shippingCalculator.calculate(eq(Province.TEHRAN), anyInt()))
                .thenReturn(new ShippingResult(ShippingZone.INTRA_PROVINCE, SHIPPING_COST));

        OrderResponseDto response = checkoutService.checkout(USER_ID, request(null));

        assertEquals(0, BigDecimal.ZERO.compareTo(response.getDiscountAmount()));
        // total = itemsCost 200 + shipping, no discount
        assertEquals(0, SHIPPING_COST.add(BigDecimal.valueOf(200)).compareTo(response.getTotalCost()));
        verifyNoInteractions(discountService);
    }

    @Test
    void checkout_with_blank_code_is_treated_as_no_code() {
        stubCartAndProduct();
        when(shippingCalculator.calculate(eq(Province.TEHRAN), anyInt()))
                .thenReturn(new ShippingResult(ShippingZone.INTRA_PROVINCE, SHIPPING_COST));

        checkoutService.checkout(USER_ID, request("   "));

        verifyNoInteractions(discountService);
    }

    @Test
    void checkout_with_invalid_code_propagates_error_without_saving() {
        stubCartAndProduct();
        when(discountService.redeemForOrder(eq("BAD"), eq(USER_ID), anyList()))
                .thenThrow(new EcommerceException(ECOMErrorType.DISCOUNT_CODE_INVALID));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> checkoutService.checkout(USER_ID, request("BAD")));

        assertEquals(ECOMErrorType.DISCOUNT_CODE_INVALID, ex.getEcomErrorType());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_when_redemption_rejected_aborts_without_saving() {
        stubCartAndProduct();
        when(discountService.redeemForOrder(eq("SAVE20"), eq(USER_ID), anyList()))
                .thenThrow(new EcommerceException(ECOMErrorType.DISCOUNT_USAGE_LIMIT_REACHED));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> checkoutService.checkout(USER_ID, request("SAVE20")));

        assertEquals(ECOMErrorType.DISCOUNT_USAGE_LIMIT_REACHED, ex.getEcomErrorType());
        verify(orderRepository, never()).save(any());
    }
}
