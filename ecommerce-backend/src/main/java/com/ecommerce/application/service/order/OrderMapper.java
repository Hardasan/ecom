package com.ecommerce.application.service.order;

import com.ecommerce.application.api.dto.order.OrderItemResponseDto;
import com.ecommerce.application.api.dto.order.OrderResponseDto;
import com.ecommerce.persistence.entity.Order;
import com.ecommerce.persistence.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface OrderMapper {

    @Mapping(target = "recipientFirstName", source = "shippingAddress.recipientFirstName")
    @Mapping(target = "recipientLastName", source = "shippingAddress.recipientLastName")
    @Mapping(target = "recipientMobile", source = "shippingAddress.recipientMobile")
    @Mapping(target = "recipientNationalId", source = "shippingAddress.recipientNationalId")
    @Mapping(target = "province", source = "shippingAddress.province")
    @Mapping(target = "city", source = "shippingAddress.city")
    @Mapping(target = "postalCode", source = "shippingAddress.postalCode")
    @Mapping(target = "addressLine", source = "shippingAddress.addressLine")
    @Mapping(target = "plaque", source = "shippingAddress.plaque")
    @Mapping(target = "unit", source = "shippingAddress.unit")
    OrderResponseDto toResponseDto(Order order);

    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "productCode", source = "product.productCode")
    OrderItemResponseDto toItemDto(OrderItem item);
}
