package com.ecommerce.application.service.address;

import com.ecommerce.application.api.dto.address.AddressResponseDto;
import com.ecommerce.persistence.entity.UserAddress;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddressService_createUTest extends BaseAddressServiceUTest {

    @Test
    void first_address_is_forced_to_default() {
        when(userAddressRepository.findByUserIdOrderByIsDefaultDescIdDesc(USER_ID)).thenReturn(List.of());

        AddressResponseDto response = addressService.create(USER_ID, request(null));

        assertTrue(response.getIsDefault());
        assertEquals("Home", response.getTitle());
        assertEquals(USER_ID, captureSaved().getUserId());
    }

    @Test
    void subsequent_non_default_address_is_not_default_and_keeps_others_intact() {
        when(userAddressRepository.findByUserIdOrderByIsDefaultDescIdDesc(USER_ID))
                .thenReturn(List.of(address(1L, true)));

        AddressResponseDto response = addressService.create(USER_ID, request(false));

        assertFalse(response.getIsDefault());
        verify(userAddressRepository, never()).findByUserIdAndIsDefaultTrue(USER_ID);
    }

    @Test
    void explicitly_default_address_clears_previous_defaults() {
        UserAddress previousDefault = address(1L, true);
        when(userAddressRepository.findByUserIdOrderByIsDefaultDescIdDesc(USER_ID))
                .thenReturn(List.of(previousDefault));
        when(userAddressRepository.findByUserIdAndIsDefaultTrue(USER_ID))
                .thenReturn(List.of(previousDefault));

        AddressResponseDto response = addressService.create(USER_ID, request(true));

        assertTrue(response.getIsDefault());
        assertFalse(previousDefault.getIsDefault());
        verify(userAddressRepository).saveAll(List.of(previousDefault));
    }

    private UserAddress captureSaved() {
        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        verify(userAddressRepository).save(captor.capture());
        return captor.getValue();
    }
}
