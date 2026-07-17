package com.ecommerce.application.integration.checkout;

import com.ecommerce.application.api.dto.address.AddressRequestDto;
import com.ecommerce.application.api.dto.order.GuestCheckoutRequestDto;
import com.ecommerce.application.api.dto.order.GuestItemRequestDto;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.Province;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GuestCheckoutITest extends AbstractCheckoutITest {

    private GuestCheckoutRequestDto guestRequest(String mobile, Long productId, int quantity, Province province) {
        GuestCheckoutRequestDto dto = new GuestCheckoutRequestDto();
        dto.setFirstName("Sara");
        dto.setLastName("Ahmadi");
        dto.setMobile(mobile);
        dto.setEmail("sara@example.com");
        dto.setNationalId("0011223344");

        AddressRequestDto address = addressRequest(province);
        address.setRecipientFirstName("Sara");
        address.setRecipientLastName("Ahmadi");
        address.setRecipientMobile(mobile);
        dto.setAddress(address);

        GuestItemRequestDto item = new GuestItemRequestDto();
        item.setProductId(productId);
        item.setVariantType(DEFAULT_VARIANT_TYPE);
        item.setVariantValue(DEFAULT_VARIANT_VALUE);
        item.setQuantity(quantity);
        dto.setItems(List.of(item));
        return dto;
    }

    private GuestCheckoutRequestDto guestRequestWithItems(String mobile,
            List<GuestItemRequestDto> items, Province province) {
        GuestCheckoutRequestDto dto = new GuestCheckoutRequestDto();
        dto.setFirstName("Sara");
        dto.setLastName("Ahmadi");
        dto.setMobile(mobile);
        dto.setEmail("sara@example.com");
        dto.setNationalId("0011223344");

        AddressRequestDto address = addressRequest(province);
        address.setRecipientFirstName("Sara");
        address.setRecipientLastName("Ahmadi");
        address.setRecipientMobile(mobile);
        dto.setAddress(address);

        dto.setItems(items);
        return dto;
    }

    private GuestItemRequestDto item(Long productId, String variantValue, int quantity) {
        GuestItemRequestDto item = new GuestItemRequestDto();
        item.setProductId(productId);
        item.setVariantType(DEFAULT_VARIANT_TYPE);
        item.setVariantValue(variantValue);
        item.setQuantity(quantity);
        return item;
    }

    @Test
    void guest_checkout_creates_unregistered_user_address_and_order() throws Exception {
        Long productId = createActiveProduct("guest-laptop", 10, 500);
        String mobile = newMobile();

        guestCheckout(guestRequest(mobile, productId, 2, Province.TEHRAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.itemsCost").value(200.0))
                .andExpect(jsonPath("$.shippingZone").value("INTRA_PROVINCE"))
                .andExpect(jsonPath("$.totalCost").value(183200.0))
                .andExpect(jsonPath("$.recipientFirstName").value("Sara"));

        // a guest AppUser was persisted with isRegistered = false
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE mobile = ?", Long.class, mobile);
        Boolean isRegistered = jdbcTemplate.queryForObject(
                "SELECT is_registered FROM app_user WHERE id = ?", Boolean.class, userId);
        assertEquals(false, isRegistered);

        // address + order persisted under the guest user
        Integer addressCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_address WHERE user_id = ?", Integer.class, userId);
        assertEquals(1, addressCount);
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE user_id = ?", Integer.class, userId);
        assertEquals(1, orderCount);

        // stock is decremented for the reservation
        assertEquals(8, inventoryOf(productId));
    }

    @Test
    void guest_can_later_complete_signup_for_the_same_mobile() throws Exception {
        Long productId = createActiveProduct("guest-phone", 10, 300);
        String mobile = newMobile();

        guestCheckout(guestRequest(mobile, productId, 1, Province.TEHRAN))
                .andExpect(status().isOk());

        // The guest mobile is not yet registered.
        clearSignupTicketState(mobile);
        // Completing the signup flow promotes the same record to a registered account.
        register(mobile);

        Boolean isRegistered = jdbcTemplate.queryForObject(
                "SELECT is_registered FROM app_user WHERE mobile = ?", Boolean.class, mobile);
        assertEquals(true, isRegistered);
        // still a single row for that mobile (the guest record was upgraded, not duplicated)
        assertEquals(1, countUsers(mobile));
    }

    @Test
    void guest_checkout_reuses_existing_unregistered_guest_without_resetting_password() throws Exception {
        Long product1 = createActiveProduct("guest-item1", 10, 300);
        String mobile = newMobile();

        guestCheckout(guestRequest(mobile, product1, 1, Province.TEHRAN))
                .andExpect(status().isOk());

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password FROM app_user WHERE mobile = ?", String.class, mobile);

        Long product2 = createActiveProduct("guest-item2", 10, 300);
        guestCheckout(guestRequest(mobile, product2, 1, Province.TEHRAN))
                .andExpect(status().isOk());

        // Same guest record reused, no duplicate
        assertEquals(1, countUsers(mobile));
        String passwordAfterReuse = jdbcTemplate.queryForObject(
                "SELECT password FROM app_user WHERE mobile = ?", String.class, mobile);
        assertEquals(passwordHash, passwordAfterReuse);
    }

    @Test
    void guest_checkout_with_already_registered_mobile_is_rejected() throws Exception {
        Long productId = createActiveProduct("guest-watch", 10, 300);
        String mobile = newMobile();
        register(mobile); // becomes a registered account

        guestCheckout(guestRequest(mobile, productId, 1, Province.TEHRAN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USER_ALREADY_EXISTS"));
    }

    @Test
    void guest_checkout_with_multiple_products_creates_separate_order_lines() throws Exception {
        Long product1 = createActiveProduct("guest-a", 10, 500);
        Long product2 = createActiveProduct("guest-b", 20, 100);
        String mobile = newMobile();

        GuestCheckoutRequestDto dto = guestRequestWithItems(mobile,
                List.of(item(product1, DEFAULT_VARIANT_VALUE, 1),
                        item(product2, DEFAULT_VARIANT_VALUE, 2)),
                Province.TEHRAN);

        guestCheckout(dto)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.itemsCost").value(300.0))
                .andExpect(jsonPath("$.totalWeightGram").value(700));
    }

    @Test
    void guest_checkout_same_product_variant_merges_quantities() throws Exception {
        Long productId = createActiveProduct("guest-merge", 10, 500);
        String mobile = newMobile();

        GuestCheckoutRequestDto dto = guestRequestWithItems(mobile,
                List.of(item(productId, DEFAULT_VARIANT_VALUE, 2),
                        item(productId, DEFAULT_VARIANT_VALUE, 3)),
                Province.TEHRAN);

        guestCheckout(dto)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.items[0].lineTotal").value(500.0));
    }

    @Test
    void guest_checkout_to_non_adjacent_province_uses_non_adjacent_tariff() throws Exception {
        Long productId = createActiveProduct("guest-fars", 10, 300);
        String mobile = newMobile();

        guestCheckout(guestRequest(mobile, productId, 1, Province.FARS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shippingZone").value("NON_ADJACENT_PROVINCE"))
                .andExpect(jsonPath("$.shippingCost").value(282200.0));
    }

    @Test
    void guest_checkout_heavy_cart_uses_over_threshold_tariff() throws Exception {
        Long productId = createActiveProduct("guest-heavy", 10, 600);
        String mobile = newMobile();

        guestCheckout(guestRequest(mobile, productId, 2, Province.TEHRAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWeightGram").value(1200))
                .andExpect(jsonPath("$.shippingCost").value(570000.0));
    }

    @Test
    void guest_checkout_with_discount_price_wins_over_unit_price() throws Exception {
        Long productId = createProductWithPrices("guest-discount", 10, 500,
                BigDecimal.valueOf(100), BigDecimal.valueOf(70));
        String mobile = newMobile();

        guestCheckout(guestRequest(mobile, productId, 2, Province.TEHRAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemsCost").value(140.0));
    }

    @Test
    void guest_checkout_with_inactive_product_returns_409() throws Exception {
        Long productId = createProduct("guest-inactive", 10, 500, ProductStatus.INACTIVE, DEFAULT_VARIANT_VALUE);
        String mobile = newMobile();

        guestCheckout(guestRequest(mobile, productId, 1, Province.TEHRAN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_AVAILABLE"));
    }

    @Test
    void guest_checkout_with_unknown_product_returns_404() throws Exception {
        guestCheckout(guestRequest(newMobile(), 999999L, 1, Province.TEHRAN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void guest_checkout_without_items_returns_400() throws Exception {
        GuestCheckoutRequestDto dto = guestRequest(newMobile(), 1L, 1, Province.TEHRAN);
        dto.setItems(List.of());

        guestCheckout(dto)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void guest_checkout_over_stock_returns_409() throws Exception {
        Long productId = createActiveProduct("guest-rare", 2, 300);

        guestCheckout(guestRequest(newMobile(), productId, 5, Province.TEHRAN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
    }
}
