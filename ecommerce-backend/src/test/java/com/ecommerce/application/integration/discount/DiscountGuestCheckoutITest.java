package com.ecommerce.application.integration.discount;

import com.ecommerce.application.api.dto.address.AddressRequestDto;
import com.ecommerce.application.api.dto.discount.CreateDiscountRequestDto;
import com.ecommerce.application.api.dto.order.GuestCheckoutRequestDto;
import com.ecommerce.application.api.dto.order.GuestItemRequestDto;
import com.ecommerce.persistence.entity.enumeration.Province;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiscountGuestCheckoutITest extends AbstractDiscountITest {

    private GuestCheckoutRequestDto guestRequest(String mobile, Long productId, int quantity, String code) {
        GuestCheckoutRequestDto dto = new GuestCheckoutRequestDto();
        dto.setFirstName("Sara");
        dto.setLastName("Ahmadi");
        dto.setMobile(mobile);
        dto.setEmail("sara@example.com");
        dto.setNationalId("0011223344");

        AddressRequestDto address = addressRequest(Province.TEHRAN);
        address.setRecipientMobile(mobile);
        dto.setAddress(address);

        GuestItemRequestDto item = new GuestItemRequestDto();
        item.setProductId(productId);
        item.setVariantType(DEFAULT_VARIANT_TYPE);
        item.setVariantValue(DEFAULT_VARIANT_VALUE);
        item.setQuantity(quantity);
        dto.setItems(List.of(item));

        dto.setDiscountCode(code);
        return dto;
    }

    @Test
    void guest_checkout_with_code_applies_discount() throws Exception {
        long discountId = createDiscount(percentage("GUEST20", 20));
        Long productId = createActiveProduct("guest-disc", 10, 500);

        JsonNode order = json(guestCheckout(guestRequest(newMobile(), productId, 2, "GUEST20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andReturn());

        assertDiscountApplied(order, "GUEST20", new BigDecimal("40.00")); // 20% of 200
        assertEquals(1, usageCount(discountId));
    }

    @Test
    void guest_checkout_with_unknown_code_is_rejected() throws Exception {
        Long productId = createActiveProduct("guest-bad", 10, 500);

        guestCheckout(guestRequest(newMobile(), productId, 1, "NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_CODE_INVALID"));

        assertEquals(10, inventoryOf(productId));
    }

    @Test
    void guest_per_user_limit_is_enforced_per_mobile() throws Exception {
        CreateDiscountRequestDto dto = percentage("GUESTONCE", 10);
        dto.setPerUserLimit(1);
        long discountId = createDiscount(dto);
        Long productId = createActiveProduct("guest-once", 10, 500);
        String mobile = newMobile();

        // Same guest mobile reuses the same (unregistered) user, so the per-user cap applies.
        guestCheckout(guestRequest(mobile, productId, 1, "GUESTONCE")).andExpect(status().isOk());
        guestCheckout(guestRequest(mobile, productId, 1, "GUESTONCE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_USAGE_LIMIT_REACHED"));

        assertEquals(1, usageCount(discountId));
    }
}
