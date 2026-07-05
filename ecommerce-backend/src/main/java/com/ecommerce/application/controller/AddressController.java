package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.address.AddressRequestDto;
import com.ecommerce.application.api.dto.address.AddressResponseDto;
import com.ecommerce.application.config.security.UserDetailsDto;
import com.ecommerce.application.service.address.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AddressResponseDto> list(Authentication authentication) {
        return addressService.list(userId(authentication));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AddressResponseDto create(@RequestBody AddressRequestDto requestDto, Authentication authentication) {
        return addressService.create(userId(authentication), requestDto);
    }

    @PutMapping(value = "/{addressId}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AddressResponseDto update(@PathVariable Long addressId, @RequestBody AddressRequestDto requestDto,
            Authentication authentication) {
        return addressService.update(userId(authentication), addressId, requestDto);
    }

    @DeleteMapping("/{addressId}")
    public void delete(@PathVariable Long addressId, Authentication authentication) {
        addressService.delete(userId(authentication), addressId);
    }

    private Long userId(Authentication authentication) {
        return ((UserDetailsDto) authentication.getPrincipal()).getId();
    }
}
