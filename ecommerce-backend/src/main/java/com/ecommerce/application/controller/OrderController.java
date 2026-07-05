package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.order.OrderResponseDto;
import com.ecommerce.application.config.security.UserDetailsDto;
import com.ecommerce.application.service.order.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CheckoutService checkoutService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OrderResponseDto> list(Authentication authentication) {
        return checkoutService.listOrders(userId(authentication));
    }

    @GetMapping(value = "/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponseDto get(@PathVariable Long orderId, Authentication authentication) {
        return checkoutService.getOrder(userId(authentication), orderId);
    }

    private Long userId(Authentication authentication) {
        return ((UserDetailsDto) authentication.getPrincipal()).getId();
    }
}
