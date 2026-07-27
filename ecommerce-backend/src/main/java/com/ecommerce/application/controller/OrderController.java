package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.order.OrderResponseDto;
import com.ecommerce.application.api.dto.order.PaymentConfirmRequestDto;
import com.ecommerce.application.api.dto.order.PaymentInitiationResponseDto;
import com.ecommerce.application.api.dto.order.RefundRequestDto;
import com.ecommerce.application.config.security.UserDetailsDto;
import com.ecommerce.application.service.order.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OrderResponseDto> list(Authentication authentication) {
        return orderService.listOrders(userId(authentication));
    }

    @GetMapping(value = "/orders/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponseDto get(@PathVariable Long orderId, Authentication authentication) {
        return orderService.getOrder(userId(authentication), orderId);
    }

    @PostMapping(value = "/orders/{orderId}/pay", produces = MediaType.APPLICATION_JSON_VALUE)
    public PaymentInitiationResponseDto pay(@PathVariable Long orderId, Authentication authentication) {
        return orderService.initiatePayment(userId(authentication), orderId);
    }

    @PostMapping(value = "/orders/{orderId}/payment/confirm",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponseDto confirmPayment(@PathVariable Long orderId,
                                           @Valid @RequestBody PaymentConfirmRequestDto requestDto) {
        return orderService.confirmPayment(orderId, requestDto);
    }

    @PostMapping(value = "/orders/{orderId}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponseDto cancel(@PathVariable Long orderId, Authentication authentication) {
        return orderService.cancelByUser(userId(authentication), orderId);
    }

    @PostMapping(value = "/orders/{orderId}/receive", produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponseDto receive(@PathVariable Long orderId, Authentication authentication) {
        return orderService.confirmReceived(userId(authentication), orderId);
    }

    @GetMapping(value = "/admin/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderResponseDto> listAll() {
        return orderService.listAllOrders();
    }

    @GetMapping(value = "/admin/orders/refundable", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderResponseDto> listRefundable() {
        return orderService.listRefundableOrders();
    }

    @GetMapping(value = "/admin/orders/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponseDto getAdmin(@PathVariable Long orderId) {
        return orderService.getOrderAdmin(orderId);
    }

    @PostMapping(value = "/admin/orders/{orderId}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponseDto cancelByAdmin(@PathVariable Long orderId) {
        return orderService.cancelByAdmin(orderId);
    }

    @PostMapping(value = "/admin/orders/{orderId}/send", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponseDto send(@PathVariable Long orderId) {
        return orderService.markSending(orderId);
    }

    @PostMapping(value = "/admin/orders/{orderId}/refund",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponseDto refund(@PathVariable Long orderId,
                                   @Valid @RequestBody RefundRequestDto requestDto) {
        return orderService.recordRefund(orderId, requestDto);
    }

    private Long userId(Authentication authentication) {
        return ((UserDetailsDto) Objects.requireNonNull(authentication.getPrincipal())).getId();
    }
}
