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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductReviewService_moderateReviewUTest extends BaseProductReviewServiceUTest {

    @Test
    void admin_approves_a_pending_review() {
        ProductReview pending = review(REVIEW_ID, PRODUCT_ID, OTHER_USER_ID, 5, ReviewStatus.PENDING, false);
        when(productReviewRepository.findByIdAndProductId(REVIEW_ID, PRODUCT_ID))
                .thenReturn(Optional.of(pending));

        ReviewResponseDto response = service.moderate(PRODUCT_ID, REVIEW_ID, ReviewStatus.PUBLISHED);

        assertEquals(ReviewStatus.PUBLISHED, response.getStatus());
        verify(productReviewRepository).save(pending);
    }

    @Test
    void admin_hides_a_published_review() {
        ProductReview published = review(REVIEW_ID, PRODUCT_ID, OTHER_USER_ID, 2, ReviewStatus.PUBLISHED, false);
        when(productReviewRepository.findByIdAndProductId(REVIEW_ID, PRODUCT_ID))
                .thenReturn(Optional.of(published));

        ReviewResponseDto response = service.moderate(PRODUCT_ID, REVIEW_ID, ReviewStatus.HIDDEN);

        assertEquals(ReviewStatus.HIDDEN, response.getStatus());
        verify(productReviewRepository).save(published);
    }

    @Test
    void admin_unhides_a_hidden_review() {
        ProductReview hidden = review(REVIEW_ID, PRODUCT_ID, OTHER_USER_ID, 2, ReviewStatus.HIDDEN, false);
        when(productReviewRepository.findByIdAndProductId(REVIEW_ID, PRODUCT_ID))
                .thenReturn(Optional.of(hidden));

        ReviewResponseDto response = service.moderate(PRODUCT_ID, REVIEW_ID, ReviewStatus.PUBLISHED);

        assertEquals(ReviewStatus.PUBLISHED, response.getStatus());
        verify(productReviewRepository).save(hidden);
    }

    @Test
    void moderating_unknown_review_throws_not_found() {
        when(productReviewRepository.findByIdAndProductId(REVIEW_ID, PRODUCT_ID))
                .thenReturn(Optional.empty());

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> service.moderate(PRODUCT_ID, REVIEW_ID, ReviewStatus.HIDDEN));

        assertEquals(ECOMErrorType.PRODUCT_REVIEW_NOT_FOUND, exception.getEcomErrorType());
        verify(productReviewRepository, never()).save(any());
    }
}
