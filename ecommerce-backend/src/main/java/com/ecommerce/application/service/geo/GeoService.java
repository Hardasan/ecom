package com.ecommerce.application.service.geo;

import com.ecommerce.application.api.dto.geo.GeoCityListResponseDto;
import com.ecommerce.application.api.dto.geo.GeoProvinceListResponseDto;
import com.ecommerce.application.api.dto.geo.SearchGeoCityRequestDto;
import com.ecommerce.persistence.repository.GeoCityRepository;
import com.ecommerce.persistence.repository.GeoProvinceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GeoService {

    private final GeoProvinceRepository geoProvinceRepository;
    private final GeoCityRepository geoCityRepository;
    private final GeoMapper geoMapper;

    @Transactional(readOnly = true)
    public GeoProvinceListResponseDto getProvinces() {
        return new GeoProvinceListResponseDto(geoMapper.toProvinceDtoList(geoProvinceRepository.findAllByOrderByNameAsc()));
    }

    @Transactional(readOnly = true)
    public GeoCityListResponseDto getCities(SearchGeoCityRequestDto searchDto) {
        return new GeoCityListResponseDto(
                geoMapper.toCityDtoList(geoCityRepository.findByProvinceOrderByNameAsc(searchDto.getProvince())));
    }
}
