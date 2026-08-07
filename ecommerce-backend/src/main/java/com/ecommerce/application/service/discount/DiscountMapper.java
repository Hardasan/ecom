package com.ecommerce.application.service.discount;

import com.ecommerce.application.api.dto.discount.CreateDiscountRequestDto;
import com.ecommerce.application.api.dto.discount.DiscountResponseDto;
import com.ecommerce.application.api.dto.discount.UpdateDiscountRequestDto;
import com.ecommerce.persistence.entity.Discount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface DiscountMapper {

    // code (normalised) and the scope collections are set explicitly in the service; usageCount and
    // audit fields are server-owned. Everything else maps by name (full replace on update).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @Mapping(target = "productIds", ignore = true)
    @Mapping(target = "categoryIds", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void apply(CreateDiscountRequestDto requestDto, @MappingTarget Discount discount);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @Mapping(target = "productIds", ignore = true)
    @Mapping(target = "categoryIds", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void apply(UpdateDiscountRequestDto requestDto, @MappingTarget Discount discount);

    DiscountResponseDto toResponseDto(Discount discount);
}
