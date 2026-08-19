package com.ecommerce.application.service.geo;

import com.ecommerce.application.api.dto.geo.SearchGeoCityRequestDto;
import com.ecommerce.persistence.entity.GeoCity;
import com.ecommerce.persistence.entity.GeoProvince;
import com.ecommerce.persistence.entity.enumeration.Province;
import com.ecommerce.persistence.repository.GeoCityRepository;
import com.ecommerce.persistence.repository.GeoProvinceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeoService_UTest {

    @Mock
    private GeoProvinceRepository geoProvinceRepository;
    @Mock
    private GeoCityRepository geoCityRepository;

    private GeoService service;

    @BeforeEach
    void setUp() {
        service = new GeoService(geoProvinceRepository, geoCityRepository, new GeoMapperImpl());
    }

    @Test
    void get_provinces_returns_all() {
        var tehran = new GeoProvince();
        tehran.setCode(Province.TEHRAN);
        tehran.setName("تهران");
        when(geoProvinceRepository.findAllByOrderByNameAsc()).thenReturn(List.of(tehran));

        var result = service.getProvinces();

        assertEquals(1, result.getProvinces().size());
        assertEquals(Province.TEHRAN, result.getProvinces().getFirst().getCode());
        assertEquals("تهران", result.getProvinces().getFirst().getName());
    }

    @Test
    void get_cities_filters_by_province() {
        var city = new GeoCity();
        city.setId(1L);
        city.setProvince(Province.TEHRAN);
        city.setName("تهران");
        when(geoCityRepository.findByProvinceOrderByNameAsc(Province.TEHRAN)).thenReturn(List.of(city));

        var search = new SearchGeoCityRequestDto();
        search.setProvince(Province.TEHRAN);

        var result = service.getCities(search);

        assertEquals(1, result.getCities().size());
        assertEquals("تهران", result.getCities().getFirst().getName());
    }
}
