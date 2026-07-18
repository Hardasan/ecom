package com.ecommerce.application.service.review;

import com.ecommerce.application.api.dto.review.ReviewSummaryResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class ProductReviewService_getSummaryUTest extends BaseProductReviewServiceUTest {

    @Test
    void computes_average_total_and_full_distribution() {
        stubProductExists();
        // Two 5-star, one 4-star, one 3-star -> total 4, weighted 17, average 4.25 -> 4.3 (HALF_UP, scale 1).
        when(productReviewRepository.countPublishedGroupedByRating(PRODUCT_ID)).thenReturn(List.of(
                new Object[]{5, 2L},
                new Object[]{4, 1L},
                new Object[]{3, 1L}));

        ReviewSummaryResponseDto summary = service.getSummary(PRODUCT_ID);

        assertEquals(PRODUCT_ID, summary.getProductId());
        assertEquals(4, summary.getTotalCount());
        assertEquals(0, summary.getAverageRating().compareTo(new BigDecimal("4.3")));
        assertEquals(2L, summary.getRatingCounts().get(5));
        assertEquals(1L, summary.getRatingCounts().get(4));
        assertEquals(1L, summary.getRatingCounts().get(3));
        assertEquals(0L, summary.getRatingCounts().get(2));
        assertEquals(0L, summary.getRatingCounts().get(1));
    }

    @Test
    void empty_product_returns_zero_average_and_zeroed_distribution() {
        stubProductExists();
        when(productReviewRepository.countPublishedGroupedByRating(PRODUCT_ID)).thenReturn(List.of());

        ReviewSummaryResponseDto summary = service.getSummary(PRODUCT_ID);

        assertEquals(0, summary.getTotalCount());
        assertEquals(0, summary.getAverageRating().compareTo(new BigDecimal("0.0")));
        for (int star = 1; star <= 5; star++) {
            assertEquals(0L, summary.getRatingCounts().get(star));
        }
    }

    @Test
    void unknown_product_throws_product_not_found() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(false);

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> service.getSummary(PRODUCT_ID));

        assertEquals(ECOMErrorType.PRODUCT_NOT_FOUND, exception.getEcomErrorType());
    }
}
