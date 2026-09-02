package com.ecommerce.application.integration.warehouse;

import com.ecommerce.application.api.dto.order.PaymentConfirmRequestDto;
import com.ecommerce.application.integration.checkout.AbstractCheckoutITest;
import com.ecommerce.persistence.entity.enumeration.Province;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end fulfillment lifecycle driven by a warehouse operator over the real HTTP layer:
 * approve -> ship -> deliver, plus cancellation, the cash-on-delivery variant, invalid transitions
 * and role security.
 */
class WarehouseFulfillmentITest extends AbstractCheckoutITest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String warehouseToken;

    @BeforeEach
    void setupWarehouseOperator() throws Exception {
        String mobile = "09100000001";
        jdbcTemplate.update(
                "INSERT INTO app_user (first_name, last_name, username, mobile, password, role, is_enabled, is_registered) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "Ware", "House", mobile, mobile,
                passwordEncoder.encode("Ware123!"), "ROLE_WAREHOUSE", true, true);
        warehouseToken = login(mobile, "Ware123!");
    }

    // ---------------------------------------------------------------------------------------------
    // Happy paths
    // ---------------------------------------------------------------------------------------------

    @Test
    void online_order_approve_ship_deliver() throws Exception {
        Long productId = createActiveProduct("wh-happy", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 2);
        payAndConfirm(userToken, orderId);
        assertEquals(8, inventoryOf(productId));

        approve(orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.approvedAt").value(notNullValue()))
                .andExpect(jsonPath("$.fulfilledByUserId").value(notNullValue()));

        ship(orderId, "Tipax", "TRK-777")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENDING"))
                .andExpect(jsonPath("$.carrier").value("Tipax"))
                .andExpect(jsonPath("$.trackingNumber").value("TRK-777"))
                .andExpect(jsonPath("$.shippedAt").value(notNullValue()));

        deliver(orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.deliveredAt").value(notNullValue()));

        assertOrderStatus(orderId, "RECEIVED");
        assertEquals(8, inventoryOf(productId));
    }

    @Test
    void cash_on_delivery_order_approve_ship_deliver() throws Exception {
        Long productId = createActiveProduct("wh-cod", 10, 500);
        long orderId = placeCodOrder(userToken, productId, 1);
        assertOrderStatus(orderId, "RESERVED");

        approve(orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
        ship(orderId, "Post", "COD-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENDING"));
        deliver(orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void cancel_from_processing_restores_stock_and_makes_refundable() throws Exception {
        Long productId = createActiveProduct("wh-cancel", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 3);
        payAndConfirm(userToken, orderId);
        approve(orderId).andExpect(status().isOk());
        assertEquals(7, inventoryOf(productId));

        cancel(orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCEL_BY_ADMIN"));

        assertEquals(10, inventoryOf(productId));
        // Paid-then-cancelled orders surface in the admin refundable list.
        mockMvc.perform(withAuth(get("/api/admin/orders/refundable"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + orderId + ")]").exists());
    }

    // ---------------------------------------------------------------------------------------------
    // Invalid transitions
    // ---------------------------------------------------------------------------------------------

    @Test
    void ship_before_approve_is_rejected() throws Exception {
        Long productId = createActiveProduct("wh-ship-early", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);
        payAndConfirm(userToken, orderId);

        ship(orderId, "Tipax", "TRK-1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_INVALID_STATUS"));
        assertOrderStatus(orderId, "PAID");
    }

    @Test
    void approve_online_order_before_payment_is_rejected() throws Exception {
        Long productId = createActiveProduct("wh-approve-unpaid", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);

        approve(orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_INVALID_STATUS"));
        assertOrderStatus(orderId, "RESERVED");
    }

    @Test
    void deliver_before_ship_and_approve_after_ship_are_rejected() throws Exception {
        Long productId = createActiveProduct("wh-bad-order", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);
        payAndConfirm(userToken, orderId);
        approve(orderId).andExpect(status().isOk());

        deliver(orderId) // still PROCESSING, not SENDING
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_INVALID_STATUS"));

        ship(orderId, "Tipax", "TRK-9").andExpect(status().isOk());

        approve(orderId) // already SENDING
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_INVALID_STATUS"));
    }

    // ---------------------------------------------------------------------------------------------
    // Security
    // ---------------------------------------------------------------------------------------------

    @Test
    void shopper_cannot_use_warehouse_endpoints() throws Exception {
        Long productId = createActiveProduct("wh-forbidden", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);
        payAndConfirm(userToken, orderId);

        mockMvc.perform(withAuth(get("/api/warehouse/orders"), userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(withAuth(post("/api/warehouse/orders/{id}/approve", orderId), userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void warehouse_staff_cannot_use_admin_only_endpoints() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/orders"), warehouseToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(withAuth(get("/api/admin/staff"), warehouseToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void warehouse_endpoints_require_authentication() throws Exception {
        mockMvc.perform(get("/api/warehouse/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_may_also_operate_the_warehouse_queue() throws Exception {
        Long productId = createActiveProduct("wh-admin-op", 10, 500);
        long orderId = placeReservedOrder(userToken, productId, 1);
        payAndConfirm(userToken, orderId);

        mockMvc.perform(withAuth(post("/api/warehouse/orders/{id}/approve", orderId), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
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
                .andReturn();
        return json(result).get("id").asLong();
    }

    private long placeCodOrder(String token, Long productId, int quantity) throws Exception {
        addToCart(token, productId, DEFAULT_VARIANT_VALUE, quantity);
        long addressId = createAddressAndGetId(token, Province.TEHRAN);
        MvcResult result = mockMvc.perform(withAuth(post("/api/checkout"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("addressId", addressId, "paymentMethod", "CASH_ON_DELIVERY"))))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("id").asLong();
    }

    private void payAndConfirm(String token, long orderId) throws Exception {
        MvcResult pay = mockMvc.perform(withAuth(post("/api/orders/{id}/pay", orderId), token))
                .andExpect(status().isOk())
                .andReturn();
        String reference = json(pay).get("paymentReference").asText();
        PaymentConfirmRequestDto body = new PaymentConfirmRequestDto();
        body.setPaymentReference(reference);
        mockMvc.perform(post("/api/orders/{id}/payment/confirm", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    private ResultActions approve(long orderId) throws Exception {
        return mockMvc.perform(withAuth(post("/api/warehouse/orders/{id}/approve", orderId), warehouseToken));
    }

    private ResultActions ship(long orderId, String carrier, String trackingNumber) throws Exception {
        return mockMvc.perform(withAuth(post("/api/warehouse/orders/{id}/ship", orderId), warehouseToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("carrier", carrier, "trackingNumber", trackingNumber))));
    }

    private ResultActions deliver(long orderId) throws Exception {
        return mockMvc.perform(withAuth(post("/api/warehouse/orders/{id}/deliver", orderId), warehouseToken));
    }

    private ResultActions cancel(long orderId) throws Exception {
        return mockMvc.perform(withAuth(post("/api/warehouse/orders/{id}/cancel", orderId), warehouseToken));
    }

    private void assertOrderStatus(long orderId, String expected) {
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE id = ?", String.class, orderId);
        assertEquals(expected, status);
    }
}
