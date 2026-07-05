package com.ecommerce.application.service.product;

import com.ecommerce.application.api.dto.product.*;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.ProductOtherImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "mainImage", ignore = true)
    @Mapping(target = "otherImages", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "weightGram", source = "weightGram", defaultValue = "0")
    void apply(CreateProductRequestDto requestDto, @MappingTarget Product product);

    GetProductResponseDto toResponseDto(Product product);

    SearchProductResponseDto toSummaryDto(Product product);

    @Mapping(target = "id", source = "id")
    ProductOtherImageDto toOtherImageDto(ProductOtherImage entity);
}
