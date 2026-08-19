package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.geo.GeoCityListResponseDto;
import com.ecommerce.application.api.dto.geo.GeoProvinceListResponseDto;
import com.ecommerce.application.api.dto.geo.SearchGeoCityRequestDto;
import com.ecommerce.application.service.geo.GeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
public class GeoController {

    private final GeoService geoService;

    @GetMapping(value = "/provinces", produces = MediaType.APPLICATION_JSON_VALUE)
    public GeoProvinceListResponseDto provinces() {
        return geoService.getProvinces();
    }

    @GetMapping(value = "/cities", produces = MediaType.APPLICATION_JSON_VALUE)
    public GeoCityListResponseDto cities(@ModelAttribute SearchGeoCityRequestDto searchDto) {
        return geoService.getCities(searchDto);
    }
}
