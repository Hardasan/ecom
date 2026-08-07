package com.ecommerce.application.integration.discount;

import com.ecommerce.application.api.dto.discount.CreateDiscountRequestDto;
import com.ecommerce.application.api.dto.discount.UpdateDiscountRequestDto;
import com.ecommerce.persistence.entity.enumeration.DiscountScope;
import com.ecommerce.persistence.entity.enumeration.DiscountType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiscountAdminCrudITest extends AbstractDiscountITest {

    // ---------------------------------------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------------------------------------

    @Test
    void create_percentage_with_cap_persists_all_fields() throws Exception {
        CreateDiscountRequestDto dto = percentage("SUMMER", 20);
        dto.setMaxDiscountAmount(BigDecimal.valueOf(100_000));
        dto.setMinimumCartAmount(BigDecimal.valueOf(500_000));
        dto.setUsageLimit(100);
        dto.setPerUserLimit(1);

        createDiscountRequest(dto, adminToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").value("SUMMER"))
                .andExpect(jsonPath("$.type").value("PERCENTAGE"))
                .andExpect(jsonPath("$.value").value(20))
                .andExpect(jsonPath("$.maxDiscountAmount").value(100000))
                .andExpect(jsonPath("$.minimumCartAmount").value(500000))
                .andExpect(jsonPath("$.usageLimit").value(100))
                .andExpect(jsonPath("$.perUserLimit").value(1))
                .andExpect(jsonPath("$.usageCount").value(0));

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM discount WHERE code = 'SUMMER'", Integer.class));
    }

    @Test
    void create_normalizes_code_to_uppercase() throws Exception {
        createDiscountRequest(percentage("  black-friday ", 30), adminToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BLACK-FRIDAY"));
    }

    @Test
    void create_products_scope_persists_targets() throws Exception {
        Long productId = createActiveProduct("disc-crud-prod", 5, 100);
        CreateDiscountRequestDto dto = discount("PRODS", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10), DiscountScope.PRODUCTS);
        dto.setProductIds(Set.of(productId));

        long id = createDiscount(dto);

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM discount_product WHERE discount_id = ? AND product_id = ?",
                Integer.class, id, productId));
    }

    @Test
    void create_categories_scope_persists_targets() throws Exception {
        CreateDiscountRequestDto dto = discount("CATS", DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(25_000), DiscountScope.CATEGORIES);
        dto.setCategoryIds(Set.of(categoryId));

        long id = createDiscount(dto);

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM discount_category WHERE discount_id = ? AND category_id = ?",
                Integer.class, id, categoryId));
    }

    @Test
    void create_duplicate_code_case_insensitive_returns_conflict() throws Exception {
        createDiscount(percentage("SAVE", 10));

        createDiscountRequest(percentage("save", 20), adminToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_CODE_ALREADY_EXISTS"));

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM discount", Integer.class));
    }

    @Test
    void create_percentage_over_100_returns_invalid_config() throws Exception {
        createDiscountRequest(percentage("TOOBIG", 150), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_INVALID_CONFIG"));
    }

    @Test
    void create_fixed_with_max_cap_returns_invalid_config() throws Exception {
        CreateDiscountRequestDto dto = fixed("FLATCAP", 50_000);
        dto.setMaxDiscountAmount(BigDecimal.valueOf(10_000));

        createDiscountRequest(dto, adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_INVALID_CONFIG"));
    }

    @Test
    void create_products_scope_without_ids_returns_invalid_config() throws Exception {
        createDiscountRequest(discount("NOIDS", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10), DiscountScope.PRODUCTS), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_INVALID_CONFIG"));
    }

    @Test
    void create_products_scope_with_unknown_product_returns_product_not_found() throws Exception {
        CreateDiscountRequestDto dto = discount("BADPROD", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10), DiscountScope.PRODUCTS);
        dto.setProductIds(Set.of(999_999L));

        createDiscountRequest(dto, adminToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void create_missing_required_fields_returns_validation_error() throws Exception {
        createDiscountRequest(new CreateDiscountRequestDto(), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void create_negative_value_returns_validation_error() throws Exception {
        createDiscountRequest(percentage("NEG", -5), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void create_requires_admin() throws Exception {
        createDiscountRequest(percentage("USERTRY", 10), userToken)
                .andExpect(status().isForbidden());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM discount", Integer.class));
    }

    @Test
    void create_without_auth_returns_401() throws Exception {
        createDiscountRequest(percentage("NOAUTH", 10), null)
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------------------------------

    @Test
    void get_by_id_returns_discount() throws Exception {
        long id = createDiscount(percentage("GETME", 15));

        mockMvc.perform(withAuth(get("/api/discounts/{id}", id), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.code").value("GETME"));
    }

    @Test
    void get_by_id_not_found_returns_404() throws Exception {
        mockMvc.perform(withAuth(get("/api/discounts/{id}", 999_999L), adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_NOT_FOUND"));
    }

    @Test
    void list_returns_all_discounts_newest_first() throws Exception {
        createDiscount(percentage("FIRST", 10));
        createDiscount(percentage("SECOND", 20));

        mockMvc.perform(withAuth(get("/api/discounts"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discounts", hasSize(2)))
                .andExpect(jsonPath("$.discounts[0].code").value("SECOND"))
                .andExpect(jsonPath("$.discounts[1].code").value("FIRST"));
    }

    @Test
    void read_requires_admin() throws Exception {
        long id = createDiscount(percentage("SECRET", 10));

        mockMvc.perform(withAuth(get("/api/discounts"), userToken)).andExpect(status().isForbidden());
        mockMvc.perform(withAuth(get("/api/discounts/{id}", id), userToken)).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------------------------------------

    @Test
    void update_replaces_configuration() throws Exception {
        long id = createDiscount(percentage("OLD", 10));

        UpdateDiscountRequestDto dto = new UpdateDiscountRequestDto();
        dto.setCode("new");
        dto.setType(DiscountType.FIXED_AMOUNT);
        dto.setValue(BigDecimal.valueOf(30_000));
        dto.setScope(DiscountScope.ALL);

        mockMvc.perform(withAuth(put("/api/discounts/{id}", id), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NEW"))
                .andExpect(jsonPath("$.type").value("FIXED_AMOUNT"))
                .andExpect(jsonPath("$.value").value(30000));

        assertEquals("NEW", jdbcTemplate.queryForObject(
                "SELECT code FROM discount WHERE id = ?", String.class, id));
    }

    @Test
    void update_switches_scope_and_replaces_targets() throws Exception {
        Long productA = createActiveProduct("disc-upd-a", 5, 100);
        Long productB = createActiveProduct("disc-upd-b", 5, 100);
        CreateDiscountRequestDto create = discount("SCOPED", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10), DiscountScope.PRODUCTS);
        create.setProductIds(Set.of(productA));
        long id = createDiscount(create);

        UpdateDiscountRequestDto dto = new UpdateDiscountRequestDto();
        dto.setCode("SCOPED");
        dto.setType(DiscountType.PERCENTAGE);
        dto.setValue(BigDecimal.valueOf(10));
        dto.setScope(DiscountScope.PRODUCTS);
        dto.setProductIds(Set.of(productB));

        mockMvc.perform(withAuth(put("/api/discounts/{id}", id), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productIds", containsInAnyOrder(productB.intValue())));

        assertEquals(productB, jdbcTemplate.queryForObject(
                "SELECT product_id FROM discount_product WHERE discount_id = ?", Long.class, id));
    }

    @Test
    void update_preserves_usage_count() throws Exception {
        long id = createDiscount(percentage("KEEP", 10));
        jdbcTemplate.update("UPDATE discount SET usage_count = 4 WHERE id = ?", id);

        mockMvc.perform(withAuth(put("/api/discounts/{id}", id), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(percentage("KEEP", 25))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(25))
                .andExpect(jsonPath("$.usageCount").value(4));
    }

    @Test
    void update_duplicate_code_returns_conflict() throws Exception {
        createDiscount(percentage("TAKEN", 10));
        long id = createDiscount(percentage("MINE", 10));

        mockMvc.perform(withAuth(put("/api/discounts/{id}", id), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(percentage("taken", 15))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_CODE_ALREADY_EXISTS"));
    }

    @Test
    void update_not_found_returns_404() throws Exception {
        mockMvc.perform(withAuth(put("/api/discounts/{id}", 999_999L), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(percentage("X", 10))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_NOT_FOUND"));
    }

    @Test
    void update_requires_admin() throws Exception {
        long id = createDiscount(percentage("PROT", 10));

        mockMvc.perform(withAuth(put("/api/discounts/{id}", id), userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(percentage("PROT", 50))))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------------------------------------
    // Delete
    // ---------------------------------------------------------------------------------------------

    @Test
    void delete_removes_discount_and_targets() throws Exception {
        Long productId = createActiveProduct("disc-del", 5, 100);
        CreateDiscountRequestDto create = discount("DELME", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10), DiscountScope.PRODUCTS);
        create.setProductIds(Set.of(productId));
        long id = createDiscount(create);

        mockMvc.perform(withAuth(delete("/api/discounts/{id}", id), adminToken))
                .andExpect(status().isNoContent());

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM discount WHERE id = ?", Integer.class, id));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM discount_product WHERE discount_id = ?", Integer.class, id));

        mockMvc.perform(withAuth(get("/api/discounts/{id}", id), adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_not_found_returns_404() throws Exception {
        mockMvc.perform(withAuth(delete("/api/discounts/{id}", 999_999L), adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_NOT_FOUND"));
    }

    @Test
    void delete_requires_admin() throws Exception {
        long id = createDiscount(percentage("KEEPME", 10));

        mockMvc.perform(withAuth(delete("/api/discounts/{id}", id), userToken))
                .andExpect(status().isForbidden());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM discount WHERE id = ?", Integer.class, id));
    }
}
