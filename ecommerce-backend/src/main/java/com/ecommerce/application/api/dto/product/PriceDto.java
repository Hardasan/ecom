package com.ecommerce.application.api.dto.product;

import com.ecommerce.persistence.entity.enumeration.VariantValue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PriceDto {

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal price;

    @DecimalMin(value = "0")
    private BigDecimal discountPrice;

    private VariantValue variantValue;
}
