package com.ecommerce.application.service.review;

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

class ProductReviewService_deleteReviewUTest extends BaseProductReviewServiceUTest {

    @Test
    void owner_deletes_own_review() {
        ProductReview own = review(REVIEW_ID, PRODUCT_ID, USER_ID, 4, ReviewStatus.PUBLISHED, false);
        when(productReviewRepository.findByIdAndProductIdAndUserId(REVIEW_ID, PRODUCT_ID, USER_ID))
                .thenReturn(Optional.of(own));

        service.delete(USER_ID, false, PRODUCT_ID, REVIEW_ID);

        verify(productReviewRepository).delete(own);
    }

    @Test
    void non_owner_non_admin_cannot_delete_and_gets_not_found() {
        when(productReviewRepository.findByIdAndProductIdAndUserId(REVIEW_ID, PRODUCT_ID, USER_ID))
                .thenReturn(Optional.empty());

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> service.delete(USER_ID, false, PRODUCT_ID, REVIEW_ID));

        assertEquals(ECOMErrorType.PRODUCT_REVIEW_NOT_FOUND, exception.getEcomErrorType());
        verify(productReviewRepository, never()).delete(any(ProductReview.class));
    }

    @Test
    void admin_deletes_any_users_review() {
        ProductReview foreign = review(REVIEW_ID, PRODUCT_ID, OTHER_USER_ID, 1, ReviewStatus.PUBLISHED, false);
        when(productReviewRepository.findByIdAndProductId(REVIEW_ID, PRODUCT_ID))
                .thenReturn(Optional.of(foreign));

        service.delete(USER_ID, true, PRODUCT_ID, REVIEW_ID);

        verify(productReviewRepository).delete(foreign);
    }

    @Test
    void admin_deleting_unknown_review_gets_not_found() {
        when(productReviewRepository.findByIdAndProductId(REVIEW_ID, PRODUCT_ID))
                .thenReturn(Optional.empty());

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> service.delete(USER_ID, true, PRODUCT_ID, REVIEW_ID));

        assertEquals(ECOMErrorType.PRODUCT_REVIEW_NOT_FOUND, exception.getEcomErrorType());
        verify(productReviewRepository, never()).delete(any(ProductReview.class));
    }
}
