package com.ecommerce.application.integration.discount;

import com.ecommerce.application.api.dto.discount.CreateDiscountRequestDto;
import com.ecommerce.persistence.entity.enumeration.Province;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiscountRedemptionReleaseITest extends AbstractDiscountITest {

    @Test
    void user_cancel_releases_the_redemption_slot() throws Exception {
        long discountId = createDiscount(limited("CANCELME", 5));
        Long productId = createActiveProduct("rel-user-cancel", 10, 500);
        long orderId = reserveWithCode(userToken, productId, 1, "CANCELME");
        assertEquals(1, usageCount(discountId));

        cancelByUser(userToken, orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCEL_BY_USER"));

        assertEquals(0, usageCount(discountId));
    }

    @Test
    void admin_cancel_releases_the_redemption_slot() throws Exception {
        long discountId = createDiscount(limited("ADMINCANCEL", 5));
        Long productId = createActiveProduct("rel-admin-cancel", 10, 500);
        long orderId = reserveWithCode(userToken, productId, 1, "ADMINCANCEL");
        assertEquals(1, usageCount(discountId));

        mockMvc.perform(withAuth(post("/api/admin/orders/{id}/cancel", orderId), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCEL_BY_ADMIN"));

        assertEquals(0, usageCount(discountId));
    }

    @Test
    void reservation_expiry_releases_the_redemption_slot() throws Exception {
        long discountId = createDiscount(limited("EXPIRESLOT", 5));
        Long productId = createActiveProduct("rel-expire", 10, 500);
        long orderId = reserveWithCode(userToken, productId, 1, "EXPIRESLOT");
        assertEquals(1, usageCount(discountId));

        jdbcTemplate.update("UPDATE orders SET reserved_until = NOW() - INTERVAL '1 minute' WHERE id = ?", orderId);
        reservationReleaseService.releaseExpiredReservations();

        assertEquals(0, usageCount(discountId));
        assertEquals("FAILED", jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE id = ?", String.class, orderId));
    }

    @Test
    void per_user_slot_is_reusable_after_cancel() throws Exception {
        CreateDiscountRequestDto dto = limited("REUSE", 5);
        dto.setPerUserLimit(1);
        createDiscount(dto);
        Long productId = createActiveProduct("rel-peruser", 10, 500);

        long firstOrder = reserveWithCode(userToken, productId, 1, "REUSE");
        cancelByUser(userToken, firstOrder).andExpect(status().isOk());

        // The per-user slot was freed, so the same user can redeem again.
        long secondOrder = reserveWithCode(userToken, productId, 1, "REUSE");
        assertEquals("RESERVED", jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE id = ?", String.class, secondOrder));
    }

    @Test
    void deleting_a_used_discount_nulls_the_order_link_but_keeps_the_snapshot() throws Exception {
        long discountId = createDiscount(limited("SNAPSHOT", 5));
        Long productId = createActiveProduct("rel-delete", 10, 500);
        long orderId = reserveWithCode(userToken, productId, 2, "SNAPSHOT");

        mockMvc.perform(withAuth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/discounts/{id}", discountId), adminToken))
                .andExpect(status().isNoContent());

        // FK ON DELETE SET NULL clears the link, but the frozen code + amount remain on the order.
        assertNull(jdbcTemplate.queryForObject(
                "SELECT discount_id FROM orders WHERE id = ?", Long.class, orderId));
        assertEquals("SNAPSHOT", jdbcTemplate.queryForObject(
                "SELECT discount_code FROM orders WHERE id = ?", String.class, orderId));

        // Cancelling still works (the releaser simply no-ops on the now-null discount link).
        cancelByUser(userToken, orderId).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------------------------------------

    private CreateDiscountRequestDto limited(String code, int usageLimit) {
        CreateDiscountRequestDto dto = percentage(code, 10);
        dto.setUsageLimit(usageLimit);
        return dto;
    }

    private long reserveWithCode(String token, Long productId, int qty, String code) throws Exception {
        addToCart(token, productId, DEFAULT_VARIANT_VALUE, qty);
        long addressId = createAddressAndGetId(token, Province.TEHRAN);
        MvcResult result = checkoutWithDiscount(token, addressId, code)
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("id").asLong();
    }

    private ResultActions cancelByUser(String token, long orderId) throws Exception {
        return mockMvc.perform(withAuth(post("/api/orders/{id}/cancel", orderId), token));
    }
}
