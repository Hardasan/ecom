package com.ecommerce.persistence.entity;

import com.ecommerce.persistence.entity.enumeration.Province;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "geo_province")
@Getter
@Setter
public class GeoProvince {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, updatable = false, length = 64)
    private Province code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;
}
