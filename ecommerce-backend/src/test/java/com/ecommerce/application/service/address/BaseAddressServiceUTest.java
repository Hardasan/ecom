package com.ecommerce.application.service.address;

import com.ecommerce.application.api.dto.address.AddressRequestDto;
import com.ecommerce.persistence.entity.UserAddress;
import com.ecommerce.persistence.entity.enumeration.Province;
import com.ecommerce.persistence.repository.UserAddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
abstract class BaseAddressServiceUTest {

    protected static final Long USER_ID = 42L;

    @Mock
    protected UserAddressRepository userAddressRepository;

    protected AddressService addressService;

    @BeforeEach
    void baseSetUp() {
        addressService = new AddressService(userAddressRepository, new AddressMapperImpl());
        lenient().when(userAddressRepository.save(any(UserAddress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    protected AddressRequestDto request(Boolean isDefault) {
        AddressRequestDto dto = new AddressRequestDto();
        dto.setTitle("Home");
        dto.setRecipientFirstName("Ali");
        dto.setRecipientLastName("Rezaei");
        dto.setRecipientMobile("09120000000");
        dto.setProvince(Province.TEHRAN);
        dto.setCity("Tehran");
        dto.setPostalCode("1234567890");
        dto.setAddressLine("Valiasr St, No 1");
        dto.setIsDefault(isDefault);
        return dto;
    }

    protected UserAddress address(Long id, boolean isDefault) {
        UserAddress address = new UserAddress();
        address.setId(id);
        address.setUserId(USER_ID);
        address.setRecipientFirstName("Ali");
        address.setRecipientLastName("Rezaei");
        address.setRecipientMobile("09120000000");
        address.setProvince(Province.TEHRAN);
        address.setCity("Tehran");
        address.setPostalCode("1234567890");
        address.setAddressLine("Valiasr St, No 1");
        address.setIsDefault(isDefault);
        return address;
    }
}
