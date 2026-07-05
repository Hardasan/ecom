package com.ecommerce.application.api.dto.address;

import com.ecommerce.persistence.entity.enumeration.Province;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class AddressResponseDto {

    private Long id;

    private String title;

    private String recipientFirstName;

    private String recipientLastName;

    private String recipientMobile;

    private String recipientNationalId;

    private Province province;

    private String city;

    private String postalCode;

    private String addressLine;

    private String plaque;

    private String unit;

    private Boolean isDefault;

    private Date createdAt;

    private Date updatedAt;
}
