package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.returns.ReturnRequestResponseDto;
import com.ecommerce.application.api.dto.returns.ReturnSearchRequestDto;
import com.ecommerce.application.service.returns.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Warehouse returns (مرجوعی) console. Warehouse staff (admins too) receive the physically-returned
 * goods for an APPROVED return and accept them — which restores stock and moves the return to
 * RECEIVED so the admin can pay the refund — or reject them at inspection. Same buyer-enriched
 * projection as the admin queue; the default view is the "awaiting acceptance" (APPROVED) list.
 */
@RestController
@RequestMapping("/api/warehouse/returns")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
public class WarehouseReturnController {

    private final ReturnService returnService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ReturnRequestResponseDto> list(@ModelAttribute ReturnSearchRequestDto searchDto) {
        return returnService.listAllReturns(searchDto.getStatus());
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ReturnRequestResponseDto get(@PathVariable Long id) {
        return returnService.getReturnAdmin(id);
    }

    @PostMapping(value = "/{id}/accept", produces = MediaType.APPLICATION_JSON_VALUE)
    public ReturnRequestResponseDto accept(@PathVariable Long id) {
        return returnService.receiveByWarehouse(id);
    }

    @PostMapping(value = "/{id}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    public ReturnRequestResponseDto reject(@PathVariable Long id) {
        return returnService.rejectByWarehouse(id);
    }
}
