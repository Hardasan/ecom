package com.ecommerce.application.api.dto.geo;

import com.ecommerce.persistence.entity.enumeration.Province;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchGeoCityRequestDto {

    @NotNull
    private Province province;
}
