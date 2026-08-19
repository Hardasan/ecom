package com.ecommerce.application.service.geo;

import com.ecommerce.application.api.dto.geo.GeoCityResponseDto;
import com.ecommerce.application.api.dto.geo.GeoProvinceResponseDto;
import com.ecommerce.persistence.entity.GeoCity;
import com.ecommerce.persistence.entity.GeoProvince;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GeoMapper {

    GeoProvinceResponseDto toProvinceDto(GeoProvince province);

    List<GeoProvinceResponseDto> toProvinceDtoList(List<GeoProvince> provinces);

    GeoCityResponseDto toCityDto(GeoCity city);

    List<GeoCityResponseDto> toCityDtoList(List<GeoCity> cities);
}
