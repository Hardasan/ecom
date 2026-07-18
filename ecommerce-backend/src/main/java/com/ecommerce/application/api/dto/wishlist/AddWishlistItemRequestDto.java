package com.ecommerce.application.api.dto.wishlist;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddWishlistItemRequestDto {

    @NotNull
    private Long productId;
}
