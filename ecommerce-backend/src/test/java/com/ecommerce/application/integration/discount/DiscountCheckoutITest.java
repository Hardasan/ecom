package com.ecommerce.application.integration.discount;

import com.ecommerce.application.api.dto.discount.CreateDiscountRequestDto;
import com.ecommerce.persistence.entity.enumeration.DiscountScope;
import com.ecommerce.persistence.entity.enumeration.DiscountType;
import com.ecommerce.persistence.entity.enumeration.Province;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiscountCheckoutITest extends AbstractDiscountITest {

    @Test
    void percentage_code_reduces_total_and_increments_usage() throws Exception {
        long discountId = createDiscount(percentage("SAVE20", 20));
        Long productId = createActiveProduct("chk-pct", 10, 500);
        addToCart(userToken, productId, DEFAULT_VARIANT_VALUE, 2); // itemsCost 200
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        JsonNode order = json(checkoutWithDiscount(userToken, addressId, "SAVE20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andReturn());

        assertDiscountApplied(order, "SAVE20", new BigDecimal("40.00")); // 20% of 200
        assertEquals(1, usageCount(discountId));

        long orderId = order.get("id").asLong();
        assertEquals(discountId, jdbcTemplate.queryForObject(
                "SELECT discount_id FROM orders WHERE id = ?", Long.class, orderId));
        assertEquals("SAVE20", jdbcTemplate.queryForObject(
                "SELECT discount_code FROM orders WHERE id = ?", String.class, orderId));
    }

    @Test
    void lowercase_code_at_checkout_matches() throws Exception {
        createDiscount(percentage("SAVE20", 20));
        Long productId = createActiveProduct("chk-lower", 10, 500);
        addToCart(userToken, productId, DEFAULT_VARIANT_VALUE, 1);
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        JsonNode order = json(checkoutWithDiscount(userToken, addressId, "save20")
                .andExpect(status().isOk())
                .andReturn());

        assertDiscountApplied(order, "SAVE20", new BigDecimal("20.00")); // 20% of 100
    }

    @Test
    void fixed_amount_code_subtracts_flat_value() throws Exception {
        createDiscount(fixed("MINUS30", 30));
        Long productId = createActiveProduct("chk-fixed", 10, 500);
        addToCart(userToken, productId, DEFAULT_VARIANT_VALUE, 2); // itemsCost 200
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        JsonNode order = json(checkoutWithDiscount(userToken, addressId, "MINUS30")
                .andExpect(status().isOk())
                .andReturn());

        assertDiscountApplied(order, "MINUS30", new BigDecimal("30.00"));
    }

    @Test
    void percentage_with_cap_is_limited_to_the_cap() throws Exception {
        // 20% of 1,000 = 200, but capped at 50.
        CreateDiscountRequestDto dto = percentage("CAP50", 20);
        dto.setMaxDiscountAmount(BigDecimal.valueOf(50));
        createDiscount(dto);
        Long productId = createActiveProduct("chk-cap", 20, 500);
        addToCart(userToken, productId, DEFAULT_VARIANT_VALUE, 10); // itemsCost 1000
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        JsonNode order = json(checkoutWithDiscount(userToken, addressId, "CAP50")
                .andExpect(status().isOk())
                .andReturn());

        assertDiscountApplied(order, "CAP50", new BigDecimal("50.00"));
    }

    @Test
    void minimum_not_met_rejects_checkout_and_does_not_redeem() throws Exception {
        CreateDiscountRequestDto dto = percentage("MIN500", 10);
        dto.setMinimumCartAmount(BigDecimal.valueOf(500));
        long discountId = createDiscount(dto);
        Long productId = createActiveProduct("chk-min", 10, 500);
        addToCart(userToken, productId, DEFAULT_VARIANT_VALUE, 2); // itemsCost 200 < 500
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        checkoutWithDiscount(userToken, addressId, "MIN500")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_MINIMUM_NOT_MET"));

        assertEquals(0, usageCount(discountId));
        assertEquals(10, inventoryOf(productId));      // nothing reserved
        assertEquals(0, orderCountForDiscount(discountId));
    }

    @Test
    void product_scoped_code_discounts_only_eligible_lines() throws Exception {
        Long eligible = createActiveProduct("chk-scope-a", 10, 500);
        Long other = createActiveProduct("chk-scope-b", 10, 500);
        CreateDiscountRequestDto dto = discount("PRODONLY", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(50), DiscountScope.PRODUCTS);
        dto.setProductIds(Set.of(eligible));
        createDiscount(dto);

        addToCart(userToken, eligible, DEFAULT_VARIANT_VALUE, 1); // 100 eligible
        addToCart(userToken, other, DEFAULT_VARIANT_VALUE, 1);    // 100 ineligible
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        JsonNode order = json(checkoutWithDiscount(userToken, addressId, "PRODONLY")
                .andExpect(status().isOk())
                .andReturn());

        // 50% of the eligible 100 only, not of the full 200 cart.
        assertDiscountApplied(order, "PRODONLY", new BigDecimal("50.00"));
    }

    @Test
    void category_scoped_code_discounts_only_products_in_category() throws Exception {
        Long otherCategory = createCategory("Books");
        Long inCategory = createActiveProduct("chk-cat-in", 10, 500);           // uses shared categoryId
        Long outOfCategory = createProductInCategory("chk-cat-out", otherCategory, 10, 500, BigDecimal.valueOf(100));
        CreateDiscountRequestDto dto = discount("CATONLY", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(50), DiscountScope.CATEGORIES);
        dto.setCategoryIds(Set.of(categoryId));
        createDiscount(dto);

        addToCart(userToken, inCategory, DEFAULT_VARIANT_VALUE, 1);     // 100 eligible
        addToCart(userToken, outOfCategory, DEFAULT_VARIANT_VALUE, 1);  // 100 ineligible
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        JsonNode order = json(checkoutWithDiscount(userToken, addressId, "CATONLY")
                .andExpect(status().isOk())
                .andReturn());

        assertDiscountApplied(order, "CATONLY", new BigDecimal("50.00"));
    }

    @Test
    void code_not_applicable_to_any_cart_item_is_rejected() throws Exception {
        Long targeted = createActiveProduct("chk-na-target", 10, 500);
        Long inCart = createActiveProduct("chk-na-incart", 10, 500);
        CreateDiscountRequestDto dto = discount("NOMATCH", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(20), DiscountScope.PRODUCTS);
        dto.setProductIds(Set.of(targeted));
        long discountId = createDiscount(dto);

        addToCart(userToken, inCart, DEFAULT_VARIANT_VALUE, 1); // targeted product is not in the cart
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        checkoutWithDiscount(userToken, addressId, "NOMATCH")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_NOT_APPLICABLE"));
        assertEquals(0, usageCount(discountId));
    }

    @Test
    void unknown_code_is_rejected_and_no_order_created() throws Exception {
        Long productId = createActiveProduct("chk-unknown", 10, 500);
        addToCart(userToken, productId, DEFAULT_VARIANT_VALUE, 1);
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        checkoutWithDiscount(userToken, addressId, "DOESNOTEXIST")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_CODE_INVALID"));

        assertEquals(10, inventoryOf(productId));
    }

    @Test
    void expired_code_is_rejected() throws Exception {
        CreateDiscountRequestDto dto = percentage("EXPIRED", 20);
        dto.setExpiresAt(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));
        long discountId = createDiscount(dto);
        Long productId = createActiveProduct("chk-expired", 10, 500);
        addToCart(userToken, productId, DEFAULT_VARIANT_VALUE, 1);
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        checkoutWithDiscount(userToken, addressId, "EXPIRED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_EXPIRED"));
        assertEquals(0, usageCount(discountId));
    }

    @Test
    void global_usage_limit_blocks_a_second_redemption() throws Exception {
        CreateDiscountRequestDto dto = percentage("ONCE", 10);
        dto.setUsageLimit(1);
        long discountId = createDiscount(dto);
        Long productId = createActiveProduct("chk-limit", 10, 500);

        // First user redeems the only slot.
        addToCart(userToken, productId, DEFAULT_VARIANT_VALUE, 1);
        long addr1 = createAddressAndGetId(userToken, Province.TEHRAN);
        checkoutWithDiscount(userToken, addr1, "ONCE").andExpect(status().isOk());
        assertEquals(1, usageCount(discountId));

        // Second user is refused.
        String otherToken = createUserAndLogin(newMobile());
        addToCart(otherToken, productId, DEFAULT_VARIANT_VALUE, 1);
        long addr2 = createAddressAndGetId(otherToken, Province.TEHRAN);
        checkoutWithDiscount(otherToken, addr2, "ONCE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_USAGE_LIMIT_REACHED"));

        assertEquals(1, usageCount(discountId));
        assertEquals(1, orderCountForDiscount(discountId));
        assertEquals(9, inventoryOf(productId)); // only the first order reserved stock
    }

    @Test
    void per_user_limit_blocks_the_same_user_twice() throws Exception {
        CreateDiscountRequestDto dto = percentage("ONEPER", 10);
        dto.setPerUserLimit(1);
        long discountId = createDiscount(dto);
        Long productId = createActiveProduct("chk-peruser", 10, 500);

        addToCart(userToken, productId, DEFAULT_VARIANT_VALUE, 1);
        long addr1 = createAddressAndGetId(userToken, Province.TEHRAN);
        checkoutWithDiscount(userToken, addr1, "ONEPER").andExpect(status().isOk());

        addToCart(userToken, productId, DEFAULT_VARIANT_VALUE, 1);
        long addr2 = createAddressAndGetId(userToken, Province.TEHRAN);
        checkoutWithDiscount(userToken, addr2, "ONEPER")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_USAGE_LIMIT_REACHED"));

        assertEquals(1, usageCount(discountId));
    }

    @Test
    void checkout_without_a_code_is_unaffected() throws Exception {
        Long productId = createActiveProduct("chk-nocode", 10, 500);
        addToCart(userToken, productId, DEFAULT_VARIANT_VALUE, 1);
        long addressId = createAddressAndGetId(userToken, Province.TEHRAN);

        // No discountCode in the body at all.
        JsonNode order = json(mockMvc.perform(withAuth(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/checkout"), userToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("addressId", addressId))))
                .andExpect(status().isOk())
                .andReturn());

        assertEquals(0, new BigDecimal("0.00").compareTo(order.get("discountAmount").decimalValue()));
    }
}
