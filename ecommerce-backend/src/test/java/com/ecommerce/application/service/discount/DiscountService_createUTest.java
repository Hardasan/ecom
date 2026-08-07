package com.ecommerce.application.service.discount;

import com.ecommerce.application.api.dto.discount.CreateDiscountRequestDto;
import com.ecommerce.application.api.dto.discount.DiscountResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.Category;
import com.ecommerce.persistence.entity.Discount;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.enumeration.DiscountScope;
import com.ecommerce.persistence.entity.enumeration.DiscountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DiscountService_createUTest extends BaseDiscountServiceUTest {

    private CreateDiscountRequestDto request(String code, DiscountType type, BigDecimal value,
            DiscountScope scope) {
        CreateDiscountRequestDto dto = new CreateDiscountRequestDto();
        dto.setCode(code);
        dto.setType(type);
        dto.setValue(value);
        dto.setScope(scope);
        return dto;
    }

    private void stubSaveWithId(long id) {
        when(discountRepository.save(any(Discount.class))).thenAnswer(inv -> {
            Discount d = inv.getArgument(0);
            d.setId(id);
            return d;
        });
    }

    @Test
    void create_normalizes_code_trims_and_uppercases() {
        CreateDiscountRequestDto dto = request("  summer24 ", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(20), DiscountScope.ALL);
        when(discountRepository.existsByCodeIgnoreCase("SUMMER24")).thenReturn(false);
        stubSaveWithId(10L);

        DiscountResponseDto response = service.create(dto);

        assertEquals(10L, response.getId());
        assertEquals("SUMMER24", response.getCode());
        assertEquals(DiscountType.PERCENTAGE, response.getType());
    }

    @Test
    void create_duplicate_code_throws() {
        CreateDiscountRequestDto dto = request("SAVE", DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50), DiscountScope.ALL);
        when(discountRepository.existsByCodeIgnoreCase("SAVE")).thenReturn(true);

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.create(dto));
        assertEquals(ECOMErrorType.DISCOUNT_CODE_ALREADY_EXISTS, ex.getEcomErrorType());
        verify(discountRepository, never()).save(any());
    }

    @Test
    void create_percentage_over_100_throws_invalid_config() {
        CreateDiscountRequestDto dto = request("BIG", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(150), DiscountScope.ALL);
        when(discountRepository.existsByCodeIgnoreCase("BIG")).thenReturn(false);

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.create(dto));
        assertEquals(ECOMErrorType.DISCOUNT_INVALID_CONFIG, ex.getEcomErrorType());
        verify(discountRepository, never()).save(any());
    }

    @Test
    void create_percentage_exactly_100_is_allowed() {
        CreateDiscountRequestDto dto = request("FREE", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(100), DiscountScope.ALL);
        when(discountRepository.existsByCodeIgnoreCase("FREE")).thenReturn(false);
        stubSaveWithId(11L);

        DiscountResponseDto response = service.create(dto);
        assertEquals(11L, response.getId());
    }

    @Test
    void create_fixed_amount_with_max_cap_throws_invalid_config() {
        CreateDiscountRequestDto dto = request("FLAT", DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50_000), DiscountScope.ALL);
        dto.setMaxDiscountAmount(BigDecimal.valueOf(10_000)); // meaningless for a flat amount
        when(discountRepository.existsByCodeIgnoreCase("FLAT")).thenReturn(false);

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.create(dto));
        assertEquals(ECOMErrorType.DISCOUNT_INVALID_CONFIG, ex.getEcomErrorType());
        verify(discountRepository, never()).save(any());
    }

    @Test
    void create_products_scope_without_ids_throws_invalid_config() {
        CreateDiscountRequestDto dto = request("PROD", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(20), DiscountScope.PRODUCTS);
        when(discountRepository.existsByCodeIgnoreCase("PROD")).thenReturn(false);

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.create(dto));
        assertEquals(ECOMErrorType.DISCOUNT_INVALID_CONFIG, ex.getEcomErrorType());
    }

    @Test
    void create_products_scope_with_unknown_product_throws_product_not_found() {
        CreateDiscountRequestDto dto = request("PROD", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(20), DiscountScope.PRODUCTS);
        dto.setProductIds(Set.of(1L, 2L));
        when(discountRepository.existsByCodeIgnoreCase("PROD")).thenReturn(false);
        when(productRepository.findAllById(any())).thenReturn(List.of(new Product())); // only 1 of 2 exist

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.create(dto));
        assertEquals(ECOMErrorType.PRODUCT_NOT_FOUND, ex.getEcomErrorType());
        verify(discountRepository, never()).save(any());
    }

    @Test
    void create_products_scope_persists_targets() {
        CreateDiscountRequestDto dto = request("PROD", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(20), DiscountScope.PRODUCTS);
        dto.setProductIds(Set.of(1L, 2L));
        when(discountRepository.existsByCodeIgnoreCase("PROD")).thenReturn(false);
        when(productRepository.findAllById(any())).thenReturn(List.of(new Product(), new Product()));
        stubSaveWithId(12L);

        DiscountResponseDto response = service.create(dto);

        assertEquals(Set.of(1L, 2L), response.getProductIds());
        assertTrue(response.getCategoryIds() == null || response.getCategoryIds().isEmpty());
    }

    @Test
    void create_categories_scope_with_unknown_category_throws_category_not_found() {
        CreateDiscountRequestDto dto = request("CAT", DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(30_000), DiscountScope.CATEGORIES);
        dto.setCategoryIds(Set.of(5L));
        when(discountRepository.existsByCodeIgnoreCase("CAT")).thenReturn(false);
        when(categoryRepository.findAllById(any())).thenReturn(List.of()); // none exist

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.create(dto));
        assertEquals(ECOMErrorType.CATEGORY_NOT_FOUND, ex.getEcomErrorType());
    }

    @Test
    void create_categories_scope_persists_targets() {
        CreateDiscountRequestDto dto = request("CAT", DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(30_000), DiscountScope.CATEGORIES);
        dto.setCategoryIds(Set.of(5L));
        when(discountRepository.existsByCodeIgnoreCase("CAT")).thenReturn(false);
        when(categoryRepository.findAllById(any())).thenReturn(List.of(new Category()));
        stubSaveWithId(13L);

        DiscountResponseDto response = service.create(dto);

        assertEquals(Set.of(5L), response.getCategoryIds());
    }

    @Test
    void create_all_scope_skips_target_validation() {
        CreateDiscountRequestDto dto = request("ALL", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(15), DiscountScope.ALL);
        when(discountRepository.existsByCodeIgnoreCase("ALL")).thenReturn(false);
        stubSaveWithId(14L);

        service.create(dto);

        verify(productRepository, never()).findAllById(any());
        verify(categoryRepository, never()).findAllById(any());
    }
}
