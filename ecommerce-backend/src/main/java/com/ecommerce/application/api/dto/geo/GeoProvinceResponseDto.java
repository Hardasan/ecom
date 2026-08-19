package com.ecommerce.application.api.dto.geo;

import com.ecommerce.persistence.entity.enumeration.Province;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeoProvinceResponseDto {

    private Province code;
    private String name;
}
