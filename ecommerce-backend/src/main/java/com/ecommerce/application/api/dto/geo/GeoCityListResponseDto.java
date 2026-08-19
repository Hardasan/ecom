package com.ecommerce.application.api.dto.geo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GeoCityListResponseDto {

    private List<GeoCityResponseDto> cities;
}
