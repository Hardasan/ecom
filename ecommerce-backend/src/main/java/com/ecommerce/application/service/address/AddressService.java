package com.ecommerce.application.service.address;

import com.ecommerce.application.api.dto.address.AddressRequestDto;
import com.ecommerce.application.api.dto.address.AddressResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.UserAddress;
import com.ecommerce.persistence.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final UserAddressRepository userAddressRepository;
    private final AddressMapper addressMapper;

    @Transactional(readOnly = true)
    public List<AddressResponseDto> list(Long userId) {
        return userAddressRepository.findByUserIdOrderByIsDefaultDescIdDesc(userId).stream()
                .map(addressMapper::toResponseDto)
                .toList();
    }

    @Transactional
    public AddressResponseDto create(Long userId, AddressRequestDto requestDto) {
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        addressMapper.apply(requestDto, address);

        boolean firstAddress = userAddressRepository.findByUserIdOrderByIsDefaultDescIdDesc(userId).isEmpty();
        boolean makeDefault = firstAddress || Boolean.TRUE.equals(requestDto.getIsDefault());
        if (makeDefault) {
            clearDefaults(userId);
        }
        address.setIsDefault(makeDefault);

        return addressMapper.toResponseDto(userAddressRepository.save(address));
    }

    @Transactional
    public AddressResponseDto update(Long userId, Long addressId, AddressRequestDto requestDto) {
        UserAddress address = findOrThrow(userId, addressId);
        addressMapper.apply(requestDto, address);

        if (Boolean.TRUE.equals(requestDto.getIsDefault())) {
            clearDefaults(userId);
            address.setIsDefault(true);
        } else if (requestDto.getIsDefault() != null) {
            address.setIsDefault(false);
        }

        return addressMapper.toResponseDto(userAddressRepository.save(address));
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        UserAddress address = findOrThrow(userId, addressId);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        userAddressRepository.delete(address);

        if (wasDefault) {
            // Promote the next most-recent address to default so the user always has one.
            userAddressRepository.findByUserIdOrderByIsDefaultDescIdDesc(userId).stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setIsDefault(true);
                        userAddressRepository.save(next);
                    });
        }
    }

    UserAddress findOrThrow(Long userId, Long addressId) {
        return userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.ADDRESS_NOT_FOUND));
    }

    private void clearDefaults(Long userId) {
        List<UserAddress> currentDefaults = userAddressRepository.findByUserIdAndIsDefaultTrue(userId);
        for (UserAddress current : currentDefaults) {
            current.setIsDefault(false);
        }
        userAddressRepository.saveAll(currentDefaults);
    }
}
