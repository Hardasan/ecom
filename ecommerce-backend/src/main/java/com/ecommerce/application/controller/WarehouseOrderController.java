package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.order.OrderResponseDto;
import com.ecommerce.application.api.dto.order.ShipOrderRequestDto;
import com.ecommerce.application.config.security.UserDetailsDto;
import com.ecommerce.application.service.order.OrderService;
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

/**
 * Warehouse fulfillment console API. Warehouse staff read the order book and drive an order through
 * fulfillment (approve -> ship -> deliver, or cancel a not-yet-dispatched order). Admins share the
 * same endpoints so they can operate the queue too. Reads reuse the admin order projection; the
 * acting staff id comes from the JWT principal, never the request body.
 */
@RestController
@RequestMapping("/api/warehouse/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
public class WarehouseOrderController {

    private final OrderService orderService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OrderResponseDto> list() {
        return orderService.listAllOrders();
    }

    @GetMapping(value = "/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponseDto get(@PathVariable Long orderId) {
        return orderService.getOrderAdmin(orderId);
    }

    @PostMapping(value = "/{orderId}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponseDto approve(@PathVariable Long orderId, Authentication authentication) {
        return orderService.approve(orderId, staffId(authentication));
    }

    @PostMapping(value = "/{orderId}/ship",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponseDto ship(@PathVariable Long orderId,
                                 @RequestBody ShipOrderRequestDto requestDto,
                                 Authentication authentication) {
        return orderService.ship(orderId, staffId(authentication),
                requestDto.getCarrier().trim(), requestDto.getTrackingNumber().trim());
    }

    @PostMapping(value = "/{orderId}/deliver", produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponseDto deliver(@PathVariable Long orderId, Authentication authentication) {
        return orderService.markDelivered(orderId, staffId(authentication));
    }

    @PostMapping(value = "/{orderId}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponseDto cancel(@PathVariable Long orderId) {
        return orderService.cancelByWarehouse(orderId);
    }

    private Long staffId(Authentication authentication) {
        return ((UserDetailsDto) Objects.requireNonNull(authentication.getPrincipal())).getId();
    }
}
