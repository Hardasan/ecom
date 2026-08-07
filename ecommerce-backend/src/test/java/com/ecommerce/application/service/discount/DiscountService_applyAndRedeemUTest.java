package com.ecommerce.application.service.discount;

import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.Discount;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class DiscountService_applyAndRedeemUTest extends BaseDiscountServiceUTest {

    private static final String CODE = "SAVE20";

    private DiscountableLine line(BigDecimal lineTotal, int quantity) {
        return new DiscountableLine(1L, 5L, null, lineTotal, quantity);
    }

    @Test
    void redeemForOrder_returns_applied_discount_and_increments_usage_under_lock() {
        Discount discount = usableDiscount(); // 20% off everything, usageCount 0
        when(discountRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.of(discount));

        AppliedDiscount applied = service.redeemForOrder("save20", USER_ID,
                List.of(line(BigDecimal.valueOf(200_000), 2)));

        assertEquals(DISCOUNT_ID, applied.discountId());
        assertEquals("SAVE20", applied.code());
        assertEquals(0, BigDecimal.valueOf(40_000).compareTo(applied.amount()));
        // Counter advanced on the locked, managed entity (flushed with the checkout transaction).
        assertEquals(1, discount.getUsageCount().intValue());
    }

    @Test
    void redeemForOrder_rejects_unknown_code() {
        when(discountRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.empty());

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.redeemForOrder(
                "save20", USER_ID, List.of(line(BigDecimal.valueOf(200_000), 2))));
        assertEquals(ECOMErrorType.DISCOUNT_CODE_INVALID, ex.getEcomErrorType());
    }

    @Test
    void redeemForOrder_rejects_expired_without_incrementing() {
        Discount discount = usableDiscount();
        discount.setExpiresAt(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)));
        when(discountRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.of(discount));

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.redeemForOrder(
                CODE, USER_ID, List.of(line(BigDecimal.valueOf(200_000), 2))));
        assertEquals(ECOMErrorType.DISCOUNT_EXPIRED, ex.getEcomErrorType());
        assertEquals(0, discount.getUsageCount().intValue());
    }

    @Test
    void redeemForOrder_rejects_when_global_limit_reached() {
        Discount discount = usableDiscount();
        discount.setUsageLimit(1);
        discount.setUsageCount(1);
        when(discountRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.of(discount));

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.redeemForOrder(
                CODE, USER_ID, List.of(line(BigDecimal.valueOf(200_000), 2))));
        assertEquals(ECOMErrorType.DISCOUNT_USAGE_LIMIT_REACHED, ex.getEcomErrorType());
        assertEquals(1, discount.getUsageCount().intValue()); // unchanged
    }

    @Test
    void redeemForOrder_rejects_when_per_user_limit_reached() {
        Discount discount = usableDiscount();
        discount.setPerUserLimit(1);
        when(discountRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.of(discount));
        when(orderRepository.countActiveByDiscountAndUser(DISCOUNT_ID, USER_ID)).thenReturn(1L);

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.redeemForOrder(
                CODE, USER_ID, List.of(line(BigDecimal.valueOf(200_000), 2))));
        assertEquals(ECOMErrorType.DISCOUNT_USAGE_LIMIT_REACHED, ex.getEcomErrorType());
    }
}
