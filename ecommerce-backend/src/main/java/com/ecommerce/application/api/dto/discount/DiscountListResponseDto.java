package com.ecommerce.application.api.dto.discount;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DiscountListResponseDto {

    private List<DiscountResponseDto> discounts;
}
