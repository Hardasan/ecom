package com.ecommerce.application.service.cart;

import com.ecommerce.application.api.dto.cart.CartItemResponseDto;
import com.ecommerce.application.api.dto.cart.CartResponseDto;
import com.ecommerce.persistence.entity.CartItem;
import com.ecommerce.persistence.entity.Product;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mapper(componentModel = "spring")
interface CartMapper {

    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "productCode", ignore = true)
    @Mapping(target = "effectivePrice", ignore = true)
    @Mapping(target = "lineTotal", ignore = true)
    CartItemResponseDto toItemDto(CartItem item, @Context Map<Long, Product> products);

    @AfterMapping
    default void enrichItem(CartItem item, @Context Map<Long, Product> products,
                            @MappingTarget CartItemResponseDto dto) {
        Product product = products.get(item.getProductId());
        if (product != null) {
            dto.setProductName(product.getName());
            dto.setProductCode(product.getCode());
        }
        BigDecimal effectivePrice = item.getDiscountPrice() != null ? item.getDiscountPrice() : item.getUnitPrice();
        dto.setEffectivePrice(effectivePrice);
        dto.setLineTotal(effectivePrice.multiply(BigDecimal.valueOf(item.getQuantity())));
    }

    default CartResponseDto toResponseDto(Long userId, List<CartItem> items, Map<Long, Product> products) {
        List<CartItemResponseDto> itemDtos = items.stream()
                .map(item -> toItemDto(item, products))
                .toList();

        CartResponseDto dto = new CartResponseDto();
        dto.setUserId(userId);
        dto.setItems(itemDtos);
        dto.setTotalQuantity(itemDtos.stream().mapToInt(CartItemResponseDto::getQuantity).sum());
        dto.setTotalPrice(itemDtos.stream()
                .map(CartItemResponseDto::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setCreatedAt(items.stream()
                .map(CartItem::getCreatedAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null));
        dto.setUpdatedAt(items.stream()
                .map(CartItem::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null));
        return dto;
    }
}
