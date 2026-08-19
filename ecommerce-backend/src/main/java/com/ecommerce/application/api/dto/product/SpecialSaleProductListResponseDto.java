package com.ecommerce.application.api.dto.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SpecialSaleProductListResponseDto {

    private List<SearchProductResponseDto> products;
}
