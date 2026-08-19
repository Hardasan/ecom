package com.ecommerce.persistence.entity;

import com.ecommerce.persistence.entity.enumeration.Province;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "geo_city")
@Getter
@Setter
public class GeoCity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "geo_city_seq")
    @SequenceGenerator(name = "geo_city_seq", sequenceName = "geo_city_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "province_code", nullable = false, length = 64)
    private Province province;

    @Column(name = "name", nullable = false, length = 128)
    private String name;
}
