package com.ecommerce.application.integration.checkout;

import com.ecommerce.application.api.dto.order.PaymentConfirmRequestDto;
import com.ecommerce.application.service.order.ReservationReleaseService;
import com.ecommerce.persistence.entity.enumeration.Province;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderLifecycleITest extends AbstractCheckoutITest {

    private static final String VALID_IBAN = "IR062960000000100324200001";

    @Autowired
    private ReservationReleaseService reservationReleaseService;

    // ---------------------------------------------------------------------------------------------
    // Happy paths
    // ---------------------------------------------------------------------------------------------

    @Test
    void happy_path_reserved_paid_sending_received_with_payment_transaction() throws Exception {
        Long productId = createActiveProduct("lifecycle-happy", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 2);
        assertEquals(8, inventoryOf(productId));
        assertTransactionCount(orderId, 0);

        String reference = initiatePay(userToken, orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentReference").isNotEmpty())
                .andExpect(jsonPath("$.redirectUrl").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String paymentReference = objectMapper.readTree(reference).get("paymentReference").asText();

        confirmPayment(orderId, paymentReference)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.reservedUntil").value(nullValue()))
                .andExpect(jsonPath("$.transactions", hasSize(1)))
                .andExpect(jsonPath("$.transactions[0].type").value("PAYMENT"))
                .andExpect(jsonPath("$.transactions[0].reference").value(paymentReference))
                .andExpect(jsonPath("$.transactions[0].iban").value(nullValue()))
                .andExpect(jsonPath("$.transactions[0].amount").value(183200.0));
        assertEquals(8, inventoryOf(productId));
        assertTransactionCount(orderId, 1);
        assertTransactionType(orderId, "PAYMENT", 1);

        adminSend(orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENDING"))
                .andExpect(jsonPath("$.transactions", hasSize(1)));

        receive(userToken, orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        mockMvc.perform(withAuth(get("/api/orders/{id}", orderId), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.transactions", hasSize(1)))
                .andExpect(jsonPath("$.transactions[0].type").value("PAYMENT"));
    }

    @Test
    void user_cancel_from_reserved_restores_inventory_without_transaction() throws Exception {
        Long productId = createActiveProduct("lifecycle-user-cancel-reserved", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 3);
        assertEquals(7, inventoryOf(productId));

        cancelByUser(userToken, orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCEL_BY_USER"))
                .andExpect(jsonPath("$.transactions", hasSize(0)));

        assertEquals(10, inventoryOf(productId));
        assertOrderStatus(orderId, "CANCEL_BY_USER");
        assertTransactionCount(orderId, 0);
    }

    @Test
    void user_cancel_from_paid_creates_refund_transaction() throws Exception {
        Long productId = createActiveProduct("lifecycle-user-cancel-paid", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 2);
        payAndConfirm(userToken, orderId);
        setIban(userToken, VALID_IBAN);
        assertTransactionCount(orderId, 1);

        cancelByUser(userToken, orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCEL_BY_USER"))
                .andExpect(jsonPath("$.transactions", hasSize(2)))
                .andExpect(jsonPath("$.transactions[0].type").value("PAYMENT"))
                .andExpect(jsonPath("$.transactions[1].type").value("REFUND"))
                .andExpect(jsonPath("$.transactions[1].iban").value(VALID_IBAN))
                .andExpect(jsonPath("$.transactions[1].amount").value(183200.0));

        assertEquals(10, inventoryOf(productId));
        assertOrderStatus(orderId, "CANCEL_BY_USER");
        assertTransactionCount(orderId, 2);
        assertTransactionType(orderId, "REFUND", 1);
    }

    @Test
    void admin_cancel_from_paid_creates_refund_transaction() throws Exception {
        Long productId = createActiveProduct("lifecycle-admin-cancel", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 2);
        payAndConfirm(userToken, orderId);
        setIban(userToken, VALID_IBAN);

        cancelByAdmin(orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCEL_BY_ADMIN"))
                .andExpect(jsonPath("$.transactions", hasSize(2)))
                .andExpect(jsonPath("$.transactions[1].type").value("REFUND"))
                .andExpect(jsonPath("$.transactions[1].iban").value(VALID_IBAN));

        assertEquals(10, inventoryOf(productId));
        assertOrderStatus(orderId, "CANCEL_BY_ADMIN");
        assertTransactionCount(orderId, 2);
    }

    // ---------------------------------------------------------------------------------------------
    // IBAN / refund guards
    // ---------------------------------------------------------------------------------------------

    @Test
    void cancel_paid_without_iban_is_rejected_and_keeps_payment_only() throws Exception {
        Long productId = createActiveProduct("lifecycle-no-iban", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);
        payAndConfirm(userToken, orderId);
        assertTransactionCount(orderId, 1);

        cancelByUser(userToken, orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USER_IBAN_REQUIRED"));

        assertEquals(9, inventoryOf(productId));
        assertOrderStatus(orderId, "PAID");
        assertTransactionCount(orderId, 1);
        assertTransactionType(orderId, "REFUND", 0);
    }

    @Test
    void admin_cancel_paid_without_iban_is_rejected() throws Exception {
        Long productId = createActiveProduct("lifecycle-admin-no-iban", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);
        payAndConfirm(userToken, orderId);

        cancelByAdmin(orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USER_IBAN_REQUIRED"));

        assertOrderStatus(orderId, "PAID");
        assertTransactionCount(orderId, 1);
        assertEquals(9, inventoryOf(productId));
    }

    // ---------------------------------------------------------------------------------------------
    // Reservation expiry
    // ---------------------------------------------------------------------------------------------

    @Test
    void expired_reservation_becomes_failed_without_transaction() throws Exception {
        Long productId = createActiveProduct("lifecycle-expire", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 2);
        assertEquals(8, inventoryOf(productId));

        jdbcTemplate.update("UPDATE orders SET reserved_until = NOW() - INTERVAL '1 minute' WHERE id = ?", orderId);
        reservationReleaseService.releaseExpiredReservations();

        assertEquals(10, inventoryOf(productId));
        assertOrderStatus(orderId, "FAILED");
        assertTransactionCount(orderId, 0);
    }

    @Test
    void pay_and_confirm_rejected_after_reservation_expires() throws Exception {
        Long productId = createActiveProduct("lifecycle-expired-pay", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);
        jdbcTemplate.update("UPDATE orders SET reserved_until = NOW() - INTERVAL '1 minute' WHERE id = ?", orderId);

        initiatePay(userToken, orderId)
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.errorCode").value("ORDER_RESERVATION_EXPIRED"));

        confirmPayment(orderId, "any-ref")
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.errorCode").value("ORDER_RESERVATION_EXPIRED"));

        assertEquals(9, inventoryOf(productId));
        assertOrderStatus(orderId, "RESERVED");
        assertTransactionCount(orderId, 0);
    }

    @Test
    void cancel_after_failed_is_rejected_and_does_not_double_restore() throws Exception {
        Long productId = createActiveProduct("lifecycle-no-double", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 2);

        jdbcTemplate.update("UPDATE orders SET reserved_until = NOW() - INTERVAL '1 minute' WHERE id = ?", orderId);
        reservationReleaseService.releaseExpiredReservations();
        assertEquals(10, inventoryOf(productId));

        cancelByUser(userToken, orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_INVALID_STATUS"));

        assertEquals(10, inventoryOf(productId));
        assertTransactionCount(orderId, 0);
    }

    // ---------------------------------------------------------------------------------------------
    // Invalid transitions
    // ---------------------------------------------------------------------------------------------

    @Test
    void confirm_payment_twice_is_rejected_and_keeps_single_payment_transaction() throws Exception {
        Long productId = createActiveProduct("lifecycle-double-pay", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);
        String reference = payAndConfirm(userToken, orderId);
        assertTransactionCount(orderId, 1);

        confirmPayment(orderId, reference)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_INVALID_STATUS"));

        assertOrderStatus(orderId, "PAID");
        assertTransactionCount(orderId, 1);
    }

    @Test
    void invalid_transitions_after_sending_are_rejected() throws Exception {
        Long productId = createActiveProduct("lifecycle-invalid", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);
        payAndConfirm(userToken, orderId);
        adminSend(orderId).andExpect(status().isOk());

        cancelByUser(userToken, orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_INVALID_STATUS"));

        cancelByAdmin(orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_INVALID_STATUS"));

        initiatePay(userToken, orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_INVALID_STATUS"));

        adminSend(orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_INVALID_STATUS"));

        assertOrderStatus(orderId, "SENDING");
        assertEquals(9, inventoryOf(productId));
        assertTransactionCount(orderId, 1);
    }

    @Test
    void receive_from_paid_is_rejected() throws Exception {
        Long productId = createActiveProduct("lifecycle-receive-paid", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);
        payAndConfirm(userToken, orderId);

        receive(userToken, orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_INVALID_STATUS"));
    }

    @Test
    void send_from_reserved_is_rejected() throws Exception {
        Long productId = createActiveProduct("lifecycle-send-reserved", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);

        adminSend(orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_INVALID_STATUS"));
    }

    @Test
    void confirm_payment_blank_reference_is_rejected() throws Exception {
        Long productId = createActiveProduct("lifecycle-blank-ref", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);

        PaymentConfirmRequestDto body = new PaymentConfirmRequestDto();
        body.setPaymentReference(" ");
        mockMvc.perform(post("/api/orders/{id}/payment/confirm", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        assertOrderStatus(orderId, "RESERVED");
        assertTransactionCount(orderId, 0);
    }

    // ---------------------------------------------------------------------------------------------
    // Security / ownership
    // ---------------------------------------------------------------------------------------------

    @Test
    void user_cannot_mutate_another_users_order() throws Exception {
        Long productId = createActiveProduct("lifecycle-other-user", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);
        String otherToken = registerAndLogin(newMobile());

        cancelByUser(otherToken, orderId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"));

        initiatePay(otherToken, orderId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"));

        receive(otherToken, orderId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"));
    }

    @Test
    void non_admin_cannot_use_admin_order_endpoints() throws Exception {
        Long productId = createActiveProduct("lifecycle-admin-forbidden", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);
        payAndConfirm(userToken, orderId);

        mockMvc.perform(withAuth(get("/api/admin/orders"), userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(withAuth(post("/api/admin/orders/{id}/send", orderId), userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(withAuth(post("/api/admin/orders/{id}/cancel", orderId), userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_can_list_and_get_any_order_including_transactions() throws Exception {
        Long productId = createActiveProduct("lifecycle-admin-list", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);
        payAndConfirm(userToken, orderId);

        mockMvc.perform(withAuth(get("/api/admin/orders"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(orderId))
                .andExpect(jsonPath("$[0].transactions", hasSize(1)))
                .andExpect(jsonPath("$[0].transactions[0].type").value("PAYMENT"));

        mockMvc.perform(withAuth(get("/api/admin/orders/{id}", orderId), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.transactions", hasSize(1)));
    }

    @Test
    void pay_without_auth_returns_401() throws Exception {
        mockMvc.perform(post("/api/orders/{id}/pay", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirm_payment_is_public() throws Exception {
        Long productId = createActiveProduct("lifecycle-public-confirm", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);
        MvcResult pay = initiatePay(userToken, orderId).andExpect(status().isOk()).andReturn();
        String reference = json(pay).get("paymentReference").asText();

        // no Authorization header
        confirmPayment(orderId, reference)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private long placeReservedOrder(String token, Long productId, int quantity) throws Exception {
        addToCart(token, productId, DEFAULT_VARIANT_VALUE, quantity);
        long addressId = createAddressAndGetId(token, Province.TEHRAN);
        MvcResult result = checkout(token, addressId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.transactions", hasSize(0)))
                .andReturn();
        return json(result).get("id").asLong();
    }

    private String payAndConfirm(String token, long orderId) throws Exception {
        MvcResult pay = initiatePay(token, orderId).andExpect(status().isOk()).andReturn();
        String reference = json(pay).get("paymentReference").asText();
        confirmPayment(orderId, reference)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.transactions", hasSize(1)))
                .andExpect(jsonPath("$.transactions[0].type").value("PAYMENT"));
        return reference;
    }

    private ResultActions initiatePay(String token, long orderId) throws Exception {
        return mockMvc.perform(withAuth(post("/api/orders/{id}/pay", orderId), token));
    }

    private ResultActions confirmPayment(long orderId, String paymentReference) throws Exception {
        PaymentConfirmRequestDto body = new PaymentConfirmRequestDto();
        body.setPaymentReference(paymentReference);
        return mockMvc.perform(post("/api/orders/{id}/payment/confirm", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions cancelByUser(String token, long orderId) throws Exception {
        return mockMvc.perform(withAuth(post("/api/orders/{id}/cancel", orderId), token));
    }

    private ResultActions cancelByAdmin(long orderId) throws Exception {
        return mockMvc.perform(withAuth(post("/api/admin/orders/{id}/cancel", orderId), adminToken));
    }

    private ResultActions adminSend(long orderId) throws Exception {
        return mockMvc.perform(withAuth(post("/api/admin/orders/{id}/send", orderId), adminToken));
    }

    private ResultActions receive(String token, long orderId) throws Exception {
        return mockMvc.perform(withAuth(post("/api/orders/{id}/receive", orderId), token));
    }

    private void setIban(String token, String iban) throws Exception {
        mockMvc.perform(withAuth(put("/api/user/iban"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("iban", iban))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iban").value(iban));
    }

    private void assertOrderStatus(long orderId, String expected) {
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE id = ?", String.class, orderId);
        assertEquals(expected, status);
    }

    private void assertTransactionCount(long orderId, int expected) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_transaction WHERE order_id = ?", Integer.class, orderId);
        assertEquals(expected, count);
    }

    private void assertTransactionType(long orderId, String type, int expected) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_transaction WHERE order_id = ? AND type = ?",
                Integer.class, orderId, type);
        assertEquals(expected, count);
    }
}
