package com.ecommerce.application.service.address;

import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.UserAddress;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddressService_deleteUTest extends BaseAddressServiceUTest {

    @Test
    void unknown_address_throws_address_not_found() {
        when(userAddressRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> addressService.delete(USER_ID, 99L));

        assertEquals(ECOMErrorType.ADDRESS_NOT_FOUND, exception.getEcomErrorType());
    }

    @Test
    void deleting_default_promotes_next_address_to_default() {
        UserAddress toDelete = address(5L, true);
        UserAddress next = address(3L, false);
        when(userAddressRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(toDelete));
        when(userAddressRepository.findByUserIdOrderByIsDefaultDescIdDesc(USER_ID)).thenReturn(List.of(next));

        addressService.delete(USER_ID, 5L);

        verify(userAddressRepository).delete(toDelete);
        assertTrue(next.getIsDefault());
        verify(userAddressRepository).save(next);
    }

    @Test
    void deleting_non_default_does_not_promote_anything() {
        UserAddress toDelete = address(5L, false);
        when(userAddressRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(toDelete));

        addressService.delete(USER_ID, 5L);

        verify(userAddressRepository).delete(toDelete);
        verify(userAddressRepository, never()).findByUserIdOrderByIsDefaultDescIdDesc(USER_ID);
    }
}
