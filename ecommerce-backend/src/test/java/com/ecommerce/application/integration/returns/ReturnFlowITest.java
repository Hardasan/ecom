package com.ecommerce.application.integration.returns;

import com.ecommerce.application.api.dto.order.PaymentConfirmRequestDto;
import com.ecommerce.application.integration.checkout.AbstractCheckoutITest;
import com.ecommerce.persistence.entity.enumeration.Province;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end customer returns (مرجوعی) over the real HTTP layer: a delivered order becomes
 * returnable, a request snapshots the chosen lines + refund amount + شبا, and the invariants
 * (one-per-order, RECEIVED-only, per-user ownership, item/quantity validation) hold.
 */
class ReturnFlowITest extends AbstractCheckoutITest {

    // ---------------------------------------------------------------------------------------------
    // Happy path
    // ---------------------------------------------------------------------------------------------

    @Test
    void delivered_order_is_returnable_then_request_is_created_and_order_drops_off_the_list() throws Exception {
        Long productId = createActiveProduct("ret-happy", 10, 500); // unit price 100 Rial
        long orderId = receivedOrder(userToken, productId, 2);
        long itemId = firstItemId(userToken, orderId);

        // It shows up as returnable.
        returnableOrders(userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value((int) orderId));

        // Create the return for the full quantity → refund = 2 × 100 = 200, status REQUESTED.
        MvcResult created = createReturn(userToken, Map.of(
                        "orderId", orderId,
                        "note", "بسته آسیب دیده بود",
                        "items", List.of(Map.of("orderItemId", itemId, "quantity", 2, "reason", "DEFECTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.orderId").value((int) orderId))
                .andExpect(jsonPath("$.refundAmount").value(200))
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[0].reason").value("DEFECTIVE"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andReturn();
        long returnId = json(created).get("id").asLong();

        // One request per order: it is no longer offered as returnable.
        returnableOrders(userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));

        // It appears in the user's returns list and detail.
        listReturns(userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value((int) returnId));
        mockMvc.perform(withAuth(get("/api/returns/{id}", returnId), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundAmount").value(200));
    }

    @Test
    void refund_uses_the_discounted_price_and_iban_defaults_to_the_saved_profile_iban() throws Exception {
        Long productId = createProductWithPrices("ret-disc", 10, 500,
                java.math.BigDecimal.valueOf(1000), java.math.BigDecimal.valueOf(600));
        long orderId = receivedOrder(userToken, productId, 1);
        long itemId = firstItemId(userToken, orderId);

        // Save a profile IBAN; the return should adopt it when the body omits one.
        mockMvc.perform(withAuth(put("/api/user/iban"), userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("iban", "IR062960000000100324200001"))))
                .andExpect(status().isOk());

        createReturn(userToken, Map.of(
                        "orderId", orderId,
                        "items", List.of(Map.of("orderItemId", itemId, "quantity", 1, "reason", "SIZE_OR_COLOR_MISMATCH"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundAmount").value(600)) // discountPrice wins
                .andExpect(jsonPath("$.iban").value("IR062960000000100324200001"));
    }

    // ---------------------------------------------------------------------------------------------
    // Invariants / errors
    // ---------------------------------------------------------------------------------------------

    @Test
    void a_not_yet_delivered_order_cannot_be_returned() throws Exception {
        Long productId = createActiveProduct("ret-paid", 10, 500);
        long orderId = paidOrder(userToken, productId, 1); // PAID, not RECEIVED
        long itemId = firstItemId(userToken, orderId);

        returnableOrders(userToken).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));

        createReturn(userToken, Map.of(
                        "orderId", orderId,
                        "items", List.of(Map.of("orderItemId", itemId, "quantity", 1, "reason", "OTHER"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_RETURNABLE"));
    }

    @Test
    void a_second_request_for_the_same_order_is_rejected() throws Exception {
        Long productId = createActiveProduct("ret-dup", 10, 500);
        long orderId = receivedOrder(userToken, productId, 1);
        long itemId = firstItemId(userToken, orderId);
        Map<String, Object> body = Map.of(
                "orderId", orderId,
                "items", List.of(Map.of("orderItemId", itemId, "quantity", 1, "reason", "CHANGED_MIND")));

        createReturn(userToken, body).andExpect(status().isOk());
        createReturn(userToken, body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RETURN_ALREADY_REQUESTED"));
    }

    @Test
    void returning_more_than_was_ordered_is_rejected() throws Exception {
        Long productId = createActiveProduct("ret-qty", 10, 500);
        long orderId = receivedOrder(userToken, productId, 1);
        long itemId = firstItemId(userToken, orderId);

        createReturn(userToken, Map.of(
                        "orderId", orderId,
                        "items", List.of(Map.of("orderItemId", itemId, "quantity", 5, "reason", "OTHER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("RETURN_ITEM_INVALID"));
    }

    @Test
    void an_item_from_another_order_is_rejected() throws Exception {
        Long productId = createActiveProduct("ret-foreign", 10, 500);
        long orderId = receivedOrder(userToken, productId, 1);

        createReturn(userToken, Map.of(
                        "orderId", orderId,
                        "items", List.of(Map.of("orderItemId", 999999, "quantity", 1, "reason", "OTHER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("RETURN_ITEM_INVALID"));
    }

    @Test
    void an_empty_item_list_fails_validation() throws Exception {
        Long productId = createActiveProduct("ret-empty", 10, 500);
        long orderId = receivedOrder(userToken, productId, 1);

        createReturn(userToken, Map.of("orderId", orderId, "items", List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void a_user_cannot_return_someone_elses_order() throws Exception {
        Long productId = createActiveProduct("ret-other", 10, 500);
        long orderId = receivedOrder(userToken, productId, 1);
        long itemId = firstItemId(userToken, orderId);

        String otherToken = registerAndLogin(newMobile());
        createReturn(otherToken, Map.of(
                        "orderId", orderId,
                        "items", List.of(Map.of("orderItemId", itemId, "quantity", 1, "reason", "OTHER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"));
    }

    @Test
    void returns_endpoints_require_authentication() throws Exception {
        mockMvc.perform(get("/api/returns")).andExpect(status().is4xxClientError());
        mockMvc.perform(get("/api/returns/returnable-orders")).andExpect(status().is4xxClientError());
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private long paidOrder(String token, Long productId, int quantity) throws Exception {
        addToCart(token, productId, DEFAULT_VARIANT_VALUE, quantity);
        long addressId = createAddressAndGetId(token, Province.TEHRAN);
        MvcResult reserved = checkout(token, addressId).andExpect(status().isOk()).andReturn();
        long orderId = json(reserved).get("id").asLong();
        MvcResult pay = mockMvc.perform(withAuth(post("/api/orders/{id}/pay", orderId), token))
                .andExpect(status().isOk()).andReturn();
        String reference = json(pay).get("paymentReference").asText();
        PaymentConfirmRequestDto confirm = new PaymentConfirmRequestDto();
        confirm.setPaymentReference(reference);
        mockMvc.perform(post("/api/orders/{id}/payment/confirm", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirm)))
                .andExpect(status().isOk());
        return orderId;
    }

    private long receivedOrder(String token, Long productId, int quantity) throws Exception {
        long orderId = paidOrder(token, productId, quantity);
        // The buyer can only confirm receipt from SENDING; admin ships (PAID → SENDING) first.
        mockMvc.perform(withAuth(post("/api/admin/orders/{id}/send", orderId), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENDING"));
        mockMvc.perform(withAuth(post("/api/orders/{id}/receive", orderId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
        return orderId;
    }

    private long firstItemId(String token, long orderId) throws Exception {
        MvcResult result = mockMvc.perform(withAuth(get("/api/orders/{id}", orderId), token))
                .andExpect(status().isOk()).andReturn();
        return json(result).get("items").get(0).get("id").asLong();
    }

    private ResultActions createReturn(String token, Map<String, Object> body) throws Exception {
        return mockMvc.perform(withAuth(post("/api/returns"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions returnableOrders(String token) throws Exception {
        return mockMvc.perform(withAuth(get("/api/returns/returnable-orders"), token));
    }

    private ResultActions listReturns(String token) throws Exception {
        return mockMvc.perform(withAuth(get("/api/returns"), token));
    }
}
