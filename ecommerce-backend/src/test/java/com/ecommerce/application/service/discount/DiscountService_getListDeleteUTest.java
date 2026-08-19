package com.ecommerce.application.service.discount;

import com.ecommerce.application.api.dto.discount.DiscountListResponseDto;
import com.ecommerce.application.api.dto.discount.DiscountResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.Discount;
import com.ecommerce.persistence.entity.enumeration.DiscountScope;
import com.ecommerce.persistence.entity.enumeration.DiscountType;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DiscountService_getListDeleteUTest extends BaseDiscountServiceUTest {

    @Test
    void getById_returns_discount() {
        Discount discount = discount(DISCOUNT_ID, "SAVE", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(20), DiscountScope.ALL);
        when(discountRepository.findById(DISCOUNT_ID)).thenReturn(Optional.of(discount));

        DiscountResponseDto response = service.getById(DISCOUNT_ID);

        assertEquals(DISCOUNT_ID, response.getId());
        assertEquals("SAVE", response.getCode());
    }

    @Test
    void getById_not_found_throws() {
        when(discountRepository.findById(DISCOUNT_ID)).thenReturn(Optional.empty());

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.getById(DISCOUNT_ID));
        assertEquals(ECOMErrorType.DISCOUNT_NOT_FOUND, ex.getEcomErrorType());
    }

    @Test
    void getAll_maps_repository_result() {
        Discount a = discount(1L, "A", DiscountType.PERCENTAGE, BigDecimal.valueOf(10), DiscountScope.ALL);
        Discount b = discount(2L, "B", DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(5_000), DiscountScope.ALL);
        when(discountRepository.findAll(any(Sort.class))).thenReturn(List.of(b, a));

        DiscountListResponseDto response = service.getAll();

        assertEquals(2, response.getDiscounts().size());
        assertEquals("B", response.getDiscounts().get(0).getCode());
        assertEquals("A", response.getDiscounts().get(1).getCode());
    }

    @Test
    void delete_removes_discount() {
        Discount discount = discount(DISCOUNT_ID, "SAVE", DiscountType.PERCENTAGE,
                BigDecimal.valueOf(20), DiscountScope.ALL);
        when(discountRepository.findById(DISCOUNT_ID)).thenReturn(Optional.of(discount));

        service.delete(DISCOUNT_ID);

        verify(discountRepository).delete(discount);
    }

    @Test
    void delete_not_found_throws() {
        when(discountRepository.findById(DISCOUNT_ID)).thenReturn(Optional.empty());

        EcommerceException ex = assertThrows(EcommerceException.class, () -> service.delete(DISCOUNT_ID));
        assertEquals(ECOMErrorType.DISCOUNT_NOT_FOUND, ex.getEcomErrorType());
        verify(discountRepository, never()).delete(any());
    }
}
