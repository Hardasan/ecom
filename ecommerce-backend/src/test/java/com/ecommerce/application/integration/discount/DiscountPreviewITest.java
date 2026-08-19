package com.ecommerce.application.integration.discount;

import com.ecommerce.application.api.dto.discount.CreateDiscountRequestDto;
import com.ecommerce.persistence.entity.enumeration.DiscountScope;
import com.ecommerce.persistence.entity.enumeration.DiscountType;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiscountPreviewITest extends AbstractDiscountITest {

    private static void assertAmount(long expected, JsonNode node) {
        assertEquals(0, BigDecimal.valueOf(expected).compareTo(node.decimalValue()),
                () -> "expected " + expected + " but was " + node);
    }

    @Test
    void preview_returns_computed_amounts_without_redeeming() throws Exception {
        long discountId = createDiscount(percentage("SAVE20", 20));
        Long productId = createActiveProduct("prev-ok", 10, 500);
        addToCart(userToken, productId, DEFAULT_VARIANT_VALUE, 2); // itemsCost 200

        JsonNode body = json(preview(userToken, "SAVE20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SAVE20"))
                .andExpect(jsonPath("$.type").value("PERCENTAGE"))
                .andReturn());

        assertAmount(200, body.get("itemsCost"));
        assertAmount(200, body.get("eligibleSubtotal"));
        assertAmount(40, body.get("discountAmount"));
        assertAmount(160, body.get("newItemsCost"));

        // Preview is read-only — the slot is not consumed.
        assertEquals(0, usageCount(discountId));
    }

    @Test
    void preview_product_scoped_reflects_only_eligible_lines() throws Exception {
        Long eligible = createActiveProduct("prev-scope-a", 10, 500);
        Long other = createActiveProduct("prev-scope-b", 10, 500);
        CreateDiscountRequestDto dto = discount("HALF", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(50), DiscountScope.PRODUCTS);
        dto.setProductIds(Set.of(eligible));
        createDiscount(dto);

        addToCart(userToken, eligible, DEFAULT_VARIANT_VALUE, 1);
        addToCart(userToken, other, DEFAULT_VARIANT_VALUE, 1);

        JsonNode body = json(preview(userToken, "HALF").andExpect(status().isOk()).andReturn());

        assertAmount(200, body.get("itemsCost"));        // whole cart
        assertAmount(100, body.get("eligibleSubtotal")); // only the targeted product
        assertAmount(50, body.get("discountAmount"));    // 50% of 100
        assertAmount(150, body.get("newItemsCost"));
    }

    @Test
    void preview_requires_authentication() throws Exception {
        createDiscount(percentage("SAVE20", 20));

        mockMvc.perform(post("/api/discounts/preview")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "SAVE20"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void preview_unknown_code_returns_404() throws Exception {
        Long productId = createActiveProduct("prev-unknown", 10, 500);
        addToCart(userToken, productId, DEFAULT_VARIANT_VALUE, 1);

        preview(userToken, "NOPE")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_CODE_INVALID"));
    }

    @Test
    void preview_expired_code_returns_409() throws Exception {
        CreateDiscountRequestDto dto = percentage("OLD", 20);
        dto.setExpiresAt(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));
        createDiscount(dto);
        Long productId = createActiveProduct("prev-expired", 10, 500);
        addToCart(userToken, productId, DEFAULT_VARIANT_VALUE, 1);

        preview(userToken, "OLD")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_EXPIRED"));
    }

    @Test
    void preview_below_minimum_returns_409() throws Exception {
        CreateDiscountRequestDto dto = percentage("MIN", 10);
        dto.setMinimumCartAmount(BigDecimal.valueOf(500));
        createDiscount(dto);
        Long productId = createActiveProduct("prev-min", 10, 500);
        addToCart(userToken, productId, DEFAULT_VARIANT_VALUE, 1); // 100 < 500

        preview(userToken, "MIN")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_MINIMUM_NOT_MET"));
    }
}
