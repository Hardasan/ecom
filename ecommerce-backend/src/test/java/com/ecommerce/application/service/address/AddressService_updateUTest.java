package com.ecommerce.application.service.address;

import com.ecommerce.application.api.dto.address.AddressRequestDto;
import com.ecommerce.application.api.dto.address.AddressResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.UserAddress;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddressService_updateUTest extends BaseAddressServiceUTest {

    @Test
    void unknown_address_throws_address_not_found() {
        when(userAddressRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> addressService.update(USER_ID, 99L, request(null)));

        assertEquals(ECOMErrorType.ADDRESS_NOT_FOUND, exception.getEcomErrorType());
    }

    @Test
    void updates_fields_and_keeps_default_flag_when_not_specified() {
        UserAddress existing = address(5L, true);
        when(userAddressRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(existing));

        AddressRequestDto dto = request(null);
        dto.setCity("Karaj");

        AddressResponseDto response = addressService.update(USER_ID, 5L, dto);

        assertEquals("Karaj", response.getCity());
        assertTrue(response.getIsDefault());
        verify(userAddressRepository, never()).findByUserIdAndIsDefaultTrue(USER_ID);
    }

    @Test
    void setting_default_true_clears_other_defaults() {
        UserAddress existing = address(5L, false);
        UserAddress otherDefault = address(1L, true);
        when(userAddressRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(existing));
        when(userAddressRepository.findByUserIdAndIsDefaultTrue(USER_ID)).thenReturn(List.of(otherDefault));

        AddressResponseDto response = addressService.update(USER_ID, 5L, request(true));

        assertTrue(response.getIsDefault());
        assertFalse(otherDefault.getIsDefault());
        verify(userAddressRepository).saveAll(List.of(otherDefault));
    }

    @Test
    void setting_default_false_unsets_the_flag() {
        UserAddress existing = address(5L, true);
        when(userAddressRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(existing));

        AddressResponseDto response = addressService.update(USER_ID, 5L, request(false));

        assertFalse(response.getIsDefault());
    }
}
