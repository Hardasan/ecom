package com.ecommerce.persistence.repository;

import com.ecommerce.persistence.entity.GeoCity;
import com.ecommerce.persistence.entity.enumeration.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeoCityRepository extends JpaRepository<GeoCity, Long> {

    List<GeoCity> findByProvinceOrderByNameAsc(Province province);
}
