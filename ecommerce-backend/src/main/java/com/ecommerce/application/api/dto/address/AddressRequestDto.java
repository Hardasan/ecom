package com.ecommerce.application.api.dto.address;

import com.ecommerce.persistence.entity.enumeration.Province;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequestDto {

    private String title;

    @NotBlank
    private String recipientFirstName;

    @NotBlank
    private String recipientLastName;

    @NotBlank
    private String recipientMobile;

    private String recipientNationalId;

    @NotNull
    private Province province;

    @NotBlank
    private String city;

    @NotBlank
    private String postalCode;

    @NotBlank
    private String addressLine;

    private String plaque;

    private String unit;

    private Boolean isDefault;
}
