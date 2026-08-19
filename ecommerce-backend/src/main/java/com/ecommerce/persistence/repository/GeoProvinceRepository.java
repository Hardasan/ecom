package com.ecommerce.persistence.repository;

import com.ecommerce.persistence.entity.GeoProvince;
import com.ecommerce.persistence.entity.enumeration.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeoProvinceRepository extends JpaRepository<GeoProvince, Province> {

    List<GeoProvince> findAllByOrderByNameAsc();
}
