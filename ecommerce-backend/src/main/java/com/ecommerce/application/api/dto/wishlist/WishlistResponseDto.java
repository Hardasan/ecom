package com.ecommerce.application.api.dto.wishlist;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WishlistResponseDto {

    private Long userId;

    private List<WishlistItemResponseDto> items;

    private Integer totalItems;
}
