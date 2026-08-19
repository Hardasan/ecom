package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.config.ClientConfigResponseDto;
import com.ecommerce.application.config.properties.ClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client-config")
@RequiredArgsConstructor
public class ClientConfigController {

    private final ClientProperties clientProperties;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ClientConfigResponseDto get() {
        return new ClientConfigResponseDto(clientProperties.getOtpTtlSeconds());
    }
}
