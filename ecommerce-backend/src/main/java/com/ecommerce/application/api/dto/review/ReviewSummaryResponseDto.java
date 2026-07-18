package com.ecommerce.application.api.dto.review;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Aggregate rating for a product, computed over its PUBLISHED reviews.
 * {@code ratingCounts} always carries keys 1..5 (zero-filled) so the frontend can draw a full histogram.
 */
@Getter
@Setter
public class ReviewSummaryResponseDto {

    private Long productId;

    private BigDecimal averageRating;

    private long totalCount;

    private Map<Integer, Long> ratingCounts;
}
