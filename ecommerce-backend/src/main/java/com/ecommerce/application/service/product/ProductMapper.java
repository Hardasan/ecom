package com.ecommerce.application.service.product;

import com.ecommerce.application.api.dto.product.*;
import com.ecommerce.persistence.entity.Price;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.ProductOtherImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "mainImage", ignore = true)
    @Mapping(target = "otherImages", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "prices", ignore = true)
    @Mapping(target = "weightGram", source = "weightGram", defaultValue = "0")
    void apply(CreateProductRequestDto requestDto, @MappingTarget Product product);

    GetProductResponseDto toResponseDto(Product product);

    SearchProductResponseDto toSummaryDto(Product product);

    @Mapping(target = "id", source = "id")
    ProductOtherImageDto toOtherImageDto(ProductOtherImage entity);

    /**
     * Copy DTO price rows into entity Price objects. variantValue is now String in both DTO
     * and entity, so this is a plain passthrough.
     */
    static List<Price> mapPrices(CreateProductRequestDto dto) {
        return dto.getPrices().stream().map(p -> {
            Price price = new Price();
            price.setPrice(p.getPrice());
            price.setDiscountPrice(p.getDiscountPrice());
            price.setVariantValue(p.getVariantValue());
            return price;
        }).collect(Collectors.toList());
    }
}
