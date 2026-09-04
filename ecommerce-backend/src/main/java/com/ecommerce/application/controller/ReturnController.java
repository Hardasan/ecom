package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.order.OrderResponseDto;
import com.ecommerce.application.api.dto.returns.CreateReturnRequestDto;
import com.ecommerce.application.api.dto.returns.ReturnRequestResponseDto;
import com.ecommerce.application.config.security.UserDetailsDto;
import com.ecommerce.application.service.returns.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * Customer returns (مرجوعی). All routes are authenticated (they fall under `/api/**`); the acting
 * user is taken from the JWT principal, never the body — a shopper only ever sees/creates their own
 * returns. Validation is handled by the ValidationAspect, so the body is NOT annotated with @Valid.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    /** Orders the shopper can still return (screen «مرجوعی سفارش»). */
    @GetMapping(value = "/returns/returnable-orders", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OrderResponseDto> returnableOrders(Authentication authentication) {
        return returnService.listReturnableOrders(userId(authentication));
    }

    @GetMapping(value = "/returns", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ReturnRequestResponseDto> list(Authentication authentication) {
        return returnService.listReturns(userId(authentication));
    }

    @GetMapping(value = "/returns/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ReturnRequestResponseDto get(@PathVariable Long id, Authentication authentication) {
        return returnService.getReturn(userId(authentication), id);
    }

    @PostMapping(value = "/returns", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ReturnRequestResponseDto create(@RequestBody CreateReturnRequestDto requestDto,
                                           Authentication authentication) {
        return returnService.createReturn(userId(authentication), requestDto);
    }

    private Long userId(Authentication authentication) {
        return ((UserDetailsDto) Objects.requireNonNull(authentication.getPrincipal())).getId();
    }
}
