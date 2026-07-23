package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.wishlist.AddWishlistItemRequestDto;
import com.ecommerce.application.api.dto.wishlist.WishlistContainsResponseDto;
import com.ecommerce.application.api.dto.wishlist.WishlistResponseDto;
import com.ecommerce.application.config.security.UserDetailsDto;
import com.ecommerce.application.service.wishlist.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public WishlistResponseDto getWishlist(Authentication authentication) {
        return wishlistService.getWishlist(userId(authentication));
    }

    @PostMapping(value = "/items", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public WishlistResponseDto addItem(@RequestBody AddWishlistItemRequestDto requestDto,
            Authentication authentication) {
        return wishlistService.addItem(userId(authentication), requestDto);
    }

    @DeleteMapping(value = "/items/{itemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public WishlistResponseDto removeItem(@PathVariable Long itemId, Authentication authentication) {
        return wishlistService.removeItem(userId(authentication), itemId);
    }

    @DeleteMapping(value = "/products/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public WishlistResponseDto removeByProduct(@PathVariable Long productId, Authentication authentication) {
        return wishlistService.removeByProduct(userId(authentication), productId);
    }

    @GetMapping(value = "/products/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public WishlistContainsResponseDto contains(@PathVariable Long productId, Authentication authentication) {
        return wishlistService.contains(userId(authentication), productId);
    }

    @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public WishlistResponseDto clear(Authentication authentication) {
        return wishlistService.clear(userId(authentication));
    }

    private Long userId(Authentication authentication) {
        return ((UserDetailsDto) authentication.getPrincipal()).getId();
    }
}
