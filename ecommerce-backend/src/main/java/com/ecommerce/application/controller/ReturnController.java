package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.order.OrderResponseDto;
import com.ecommerce.application.api.dto.returns.CreateReturnRequestDto;
import com.ecommerce.application.api.dto.returns.ReturnRefundRequestDto;
import com.ecommerce.application.api.dto.returns.ReturnRequestResponseDto;
import com.ecommerce.application.api.dto.returns.ReturnSearchRequestDto;
import com.ecommerce.application.config.security.UserDetailsDto;
import com.ecommerce.application.service.returns.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

    // ---- admin moderation queue (ROLE_ADMIN) -----------------------------------------------------

    /** Return queue for the dashboard; optional {@code ?status=} filters to one lifecycle state. */
    @GetMapping(value = "/admin/returns", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public List<ReturnRequestResponseDto> listAll(@ModelAttribute ReturnSearchRequestDto searchDto) {
        return returnService.listAllReturns(searchDto.getStatus());
    }

    @GetMapping(value = "/admin/returns/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ReturnRequestResponseDto getAdmin(@PathVariable Long id) {
        return returnService.getReturnAdmin(id);
    }

    @PostMapping(value = "/admin/returns/{id}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ReturnRequestResponseDto approve(@PathVariable Long id) {
        return returnService.approve(id);
    }

    @PostMapping(value = "/admin/returns/{id}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ReturnRequestResponseDto reject(@PathVariable Long id) {
        return returnService.reject(id);
    }

    @PostMapping(value = "/admin/returns/{id}/refund",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ReturnRequestResponseDto refund(@PathVariable Long id,
                                           @RequestBody ReturnRefundRequestDto requestDto) {
        return returnService.refund(id, requestDto);
    }

    private Long userId(Authentication authentication) {
        return ((UserDetailsDto) Objects.requireNonNull(authentication.getPrincipal())).getId();
    }
}
