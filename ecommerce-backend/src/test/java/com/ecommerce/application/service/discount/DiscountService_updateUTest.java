package com.ecommerce.application.service.discount;

import com.ecommerce.application.api.dto.discount.DiscountResponseDto;
import com.ecommerce.application.api.dto.discount.UpdateDiscountRequestDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.Discount;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.enumeration.DiscountScope;
import com.ecommerce.persistence.entity.enumeration.DiscountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DiscountService_updateUTest extends BaseDiscountServiceUTest {

    private UpdateDiscountRequestDto request(String code, DiscountType type, BigDecimal value,
            DiscountScope scope) {
        UpdateDiscountRequestDto dto = new UpdateDiscountRequestDto();
        dto.setCode(code);
        dto.setType(type);
        dto.setValue(value);
        dto.setScope(scope);
        return dto;
    }

    private void stubSaveReturnsArgument() {
        when(discountRepository.save(any(Discount.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void update_not_found_throws() {
        when(discountRepository.findById(DISCOUNT_ID)).thenReturn(Optional.empty());

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> service.update(DISCOUNT_ID, request("X", DiscountType.PERCENTAGE,
                        BigDecimal.TEN, DiscountScope.ALL)));
        assertEquals(ECOMErrorType.DISCOUNT_NOT_FOUND, ex.getEcomErrorType());
    }

    @Test
    void update_replaces_configuration() {
        Discount existing = discount(DISCOUNT_ID, "OLD", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10), DiscountScope.ALL);
        when(discountRepository.findById(DISCOUNT_ID)).thenReturn(Optional.of(existing));
        when(discountRepository.existsByCodeIgnoreCaseAndIdNot("NEW", DISCOUNT_ID)).thenReturn(false);
        stubSaveReturnsArgument();

        UpdateDiscountRequestDto dto = request("new", DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(25_000), DiscountScope.ALL);
        DiscountResponseDto response = service.update(DISCOUNT_ID, dto);

        assertEquals("NEW", response.getCode());
        assertEquals(DiscountType.FIXED_AMOUNT, response.getType());
        assertEquals(0, BigDecimal.valueOf(25_000).compareTo(response.getValue()));
    }

    @Test
    void update_duplicate_code_throws() {
        Discount existing = discount(DISCOUNT_ID, "OLD", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10), DiscountScope.ALL);
        when(discountRepository.findById(DISCOUNT_ID)).thenReturn(Optional.of(existing));
        when(discountRepository.existsByCodeIgnoreCaseAndIdNot("TAKEN", DISCOUNT_ID)).thenReturn(true);

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> service.update(DISCOUNT_ID, request("taken", DiscountType.PERCENTAGE,
                        BigDecimal.valueOf(10), DiscountScope.ALL)));
        assertEquals(ECOMErrorType.DISCOUNT_CODE_ALREADY_EXISTS, ex.getEcomErrorType());
        verify(discountRepository, never()).save(any());
    }

    @Test
    void update_preserves_server_owned_usage_count() {
        Discount existing = discount(DISCOUNT_ID, "OLD", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10), DiscountScope.ALL);
        existing.setUsageCount(3);
        when(discountRepository.findById(DISCOUNT_ID)).thenReturn(Optional.of(existing));
        when(discountRepository.existsByCodeIgnoreCaseAndIdNot("OLD", DISCOUNT_ID)).thenReturn(false);
        stubSaveReturnsArgument();

        DiscountResponseDto response = service.update(DISCOUNT_ID,
                request("OLD", DiscountType.PERCENTAGE, BigDecimal.valueOf(30), DiscountScope.ALL));

        assertEquals(3, response.getUsageCount().intValue());
    }

    @Test
    void update_switches_scope_and_recomputes_targets() {
        Discount existing = discount(DISCOUNT_ID, "OLD", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10), DiscountScope.ALL);
        when(discountRepository.findById(DISCOUNT_ID)).thenReturn(Optional.of(existing));
        when(discountRepository.existsByCodeIgnoreCaseAndIdNot("OLD", DISCOUNT_ID)).thenReturn(false);
        when(productRepository.findAllById(any())).thenReturn(List.of(new Product()));
        stubSaveReturnsArgument();

        UpdateDiscountRequestDto dto = request("OLD", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10), DiscountScope.PRODUCTS);
        dto.setProductIds(Set.of(42L));
        DiscountResponseDto response = service.update(DISCOUNT_ID, dto);

        assertEquals(DiscountScope.PRODUCTS, response.getScope());
        assertEquals(Set.of(42L), response.getProductIds());
    }
}
