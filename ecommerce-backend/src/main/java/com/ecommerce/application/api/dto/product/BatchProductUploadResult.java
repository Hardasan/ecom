package com.ecommerce.application.api.dto.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BatchProductUploadResult {

    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<BatchProductRowError> errors;
    private long elapsedTimeMs;
}
