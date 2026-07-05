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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Entity
@Table(name = "user_address")
@Getter
@Setter
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_address_seq")
    @SequenceGenerator(name = "user_address_seq", sequenceName = "user_address_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "recipient_first_name", nullable = false, length = 255)
    private String recipientFirstName;

    @Column(name = "recipient_last_name", nullable = false, length = 255)
    private String recipientLastName;

    @Column(name = "recipient_mobile", nullable = false, length = 20)
    private String recipientMobile;

    @Column(name = "recipient_national_id", length = 20)
    private String recipientNationalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "province", nullable = false, length = 64)
    private Province province;

    @Column(name = "city", nullable = false, length = 255)
    private String city;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "address_line", nullable = false, columnDefinition = "TEXT")
    private String addressLine;

    @Column(name = "plaque", length = 32)
    private String plaque;

    @Column(name = "unit", length = 32)
    private String unit;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Date createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Date updatedAt;
}
