package com.ecommerce.application.api.dto.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BatchProductRowError {

    private int rowNumber;
    private String field;
    private String message;
}
