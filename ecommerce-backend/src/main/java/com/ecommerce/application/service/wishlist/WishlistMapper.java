package com.ecommerce.application.service.wishlist;

import com.ecommerce.application.api.dto.product.PriceDto;
import com.ecommerce.application.api.dto.product.ProductImageDto;
import com.ecommerce.application.api.dto.wishlist.WishlistItemResponseDto;
import com.ecommerce.application.api.dto.wishlist.WishlistResponseDto;
import com.ecommerce.persistence.entity.Price;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.ProductImage;
import com.ecommerce.persistence.entity.WishlistItem;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
interface WishlistMapper {

    @Mapping(target = "addedAt", source = "createdAt")
    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "productLocalName", ignore = true)
    @Mapping(target = "productCode", ignore = true)
    @Mapping(target = "productUrl", ignore = true)
    @Mapping(target = "mainImage", ignore = true)
    @Mapping(target = "prices", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "inventoryStatus", ignore = true)
    @Mapping(target = "inventoryCount", ignore = true)
    @Mapping(target = "inStock", ignore = true)
    @Mapping(target = "available", ignore = true)
    WishlistItemResponseDto toItemDto(WishlistItem item, @Context Map<Long, Product> products);

    ProductImageDto toImageDto(ProductImage image);

    PriceDto toPriceDto(Price price);

    @AfterMapping
    default void enrichItem(WishlistItem item, @Context Map<Long, Product> products,
                            @MappingTarget WishlistItemResponseDto dto) {
        Product product = products.get(item.getProductId());
        if (product == null) {
            return;
        }
        dto.setProductName(product.getName());
        dto.setProductLocalName(product.getLocalName());
        dto.setProductCode(product.getCode());
        dto.setProductUrl(product.getUrl());
        dto.setMainImage(toImageDto(product.getMainImage()));
        dto.setPrices(product.getPrices().stream().map(this::toPriceDto).toList());
        dto.setStatus(product.getStatus());
        dto.setInventoryStatus(product.getInventoryStatus());
        dto.setInventoryCount(product.getInventoryCount());

        boolean inStock = product.getInventoryCount() != null && product.getInventoryCount() > 0;
        dto.setInStock(inStock);
        dto.setAvailable(product.getStatus() == ProductStatus.ACTIVE && inStock);
    }

    default WishlistResponseDto toResponseDto(Long userId, List<WishlistItem> items, Map<Long, Product> products) {
        List<WishlistItemResponseDto> itemDtos = items.stream()
                .map(item -> toItemDto(item, products))
                .toList();

        WishlistResponseDto dto = new WishlistResponseDto();
        dto.setUserId(userId);
        dto.setItems(itemDtos);
        dto.setTotalItems(itemDtos.size());
        return dto;
    }
}
