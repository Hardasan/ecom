package com.ecommerce.application.service.review;

import com.ecommerce.application.api.dto.review.ReviewResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.ProductReview;
import com.ecommerce.persistence.entity.enumeration.ReviewStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductReviewService_updateReviewUTest extends BaseProductReviewServiceUTest {

    @Test
    void editing_a_review_preserves_snapshot_and_resets_status_to_pending() {
        ProductReview existing = review(REVIEW_ID, PRODUCT_ID, USER_ID, 3, ReviewStatus.PUBLISHED, true);
        when(productReviewRepository.findByIdAndProductIdAndUserId(REVIEW_ID, PRODUCT_ID, USER_ID))
                .thenReturn(Optional.of(existing));

        ReviewResponseDto response = service.update(USER_ID, PRODUCT_ID, REVIEW_ID,
                request(5, "Updated title", "Updated comment"));

        assertEquals(5, response.getRating());
        assertEquals("Updated title", response.getTitle());
        assertEquals("Updated comment", response.getComment());
        // authorName / verifiedPurchase are preserved; the edited content goes back to PENDING.
        assertEquals("Amir Zaman", response.getAuthorName());
        assertEquals(ReviewStatus.PENDING, response.getStatus());
        assertTrue(response.getVerifiedPurchase());
        verify(productReviewRepository).save(existing);
    }

    @Test
    void missing_or_foreign_review_throws_review_not_found() {
        when(productReviewRepository.findByIdAndProductIdAndUserId(REVIEW_ID, PRODUCT_ID, USER_ID))
                .thenReturn(Optional.empty());

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> service.update(USER_ID, PRODUCT_ID, REVIEW_ID, request(2, "x", "y")));

        assertEquals(ECOMErrorType.PRODUCT_REVIEW_NOT_FOUND, exception.getEcomErrorType());
        verify(productReviewRepository, never()).save(any());
    }
}
