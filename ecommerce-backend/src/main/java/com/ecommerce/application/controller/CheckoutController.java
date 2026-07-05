package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.order.CheckoutRequestDto;
import com.ecommerce.application.api.dto.order.GuestCheckoutRequestDto;
import com.ecommerce.application.api.dto.order.OrderResponseDto;
import com.ecommerce.application.config.security.UserDetailsDto;
import com.ecommerce.application.service.order.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponseDto checkout(@RequestBody CheckoutRequestDto requestDto, Authentication authentication) {
        return checkoutService.checkout(userId(authentication), requestDto);
    }

    @PostMapping(value = "/guest", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponseDto guestCheckout(@RequestBody GuestCheckoutRequestDto requestDto) {
        return checkoutService.guestCheckout(requestDto);
    }

    private Long userId(Authentication authentication) {
        return ((UserDetailsDto) authentication.getPrincipal()).getId();
    }
}
