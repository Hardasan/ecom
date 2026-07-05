package com.ecommerce.application.service.address;

import com.ecommerce.application.api.dto.address.AddressRequestDto;
import com.ecommerce.application.api.dto.address.AddressResponseDto;
import com.ecommerce.persistence.entity.UserAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface AddressMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void apply(AddressRequestDto requestDto, @MappingTarget UserAddress address);

    AddressResponseDto toResponseDto(UserAddress address);
}
