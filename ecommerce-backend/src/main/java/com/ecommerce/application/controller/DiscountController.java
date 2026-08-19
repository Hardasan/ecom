package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.discount.*;
import com.ecommerce.application.config.security.UserDetailsDto;
import com.ecommerce.application.service.discount.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/discounts")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService discountService;

    // ---- Admin CRUD (ROLE_ADMIN) --------------------------------------------------------------

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountResponseDto create(@RequestBody CreateDiscountRequestDto requestDto) {
        return discountService.create(requestDto);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public DiscountListResponseDto getAll() {
        return discountService.getAll();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public DiscountResponseDto getById(@PathVariable Long id) {
        return discountService.getById(id);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public DiscountResponseDto update(@PathVariable Long id,
            @RequestBody UpdateDiscountRequestDto requestDto) {
        return discountService.update(id, requestDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        discountService.delete(id);
    }

    // ---- Customer preview (any authenticated user) --------------------------------------------

    @PostMapping(value = "/preview", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DiscountPreviewResponseDto preview(@RequestBody ApplyDiscountRequestDto requestDto,
            Authentication authentication) {
        return discountService.preview(userId(authentication), requestDto.getCode());
    }

    private Long userId(Authentication authentication) {
        return ((UserDetailsDto) Objects.requireNonNull(authentication.getPrincipal())).getId();
    }
}
