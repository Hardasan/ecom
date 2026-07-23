package com.ecommerce.application.service.review;

import com.ecommerce.application.api.dto.review.ReviewResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.ProductReview;
import com.ecommerce.persistence.entity.enumeration.ReviewStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductReviewService_createReviewUTest extends BaseProductReviewServiceUTest {

    @Test
    void creates_published_review_with_author_snapshot_and_verified_purchase() {
        stubProductExists();
        when(productReviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(false);
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(appUser(USER_ID, "Amir", "Zaman")));
        when(orderRepository.existsPaidOrderForProduct(USER_ID, PRODUCT_ID)).thenReturn(true);

        ReviewResponseDto response = service.create(USER_ID, PRODUCT_ID, request(5, "Great", "Loved it"));

        assertEquals(5, response.getRating());
        assertEquals("Great", response.getTitle());
        assertEquals("Loved it", response.getComment());
        assertEquals("Amir Zaman", response.getAuthorName());
        assertEquals(PRODUCT_ID, response.getProductId());
        assertEquals(ReviewStatus.PUBLISHED, response.getStatus());
        assertTrue(response.getVerifiedPurchase());

        ArgumentCaptor<ProductReview> saved = ArgumentCaptor.forClass(ProductReview.class);
        verify(productReviewRepository).saveAndFlush(saved.capture());
        assertEquals(USER_ID, saved.getValue().getUserId());
        assertEquals(PRODUCT_ID, saved.getValue().getProductId());
        assertEquals("Amir Zaman", saved.getValue().getAuthorName());
        assertEquals(ReviewStatus.PUBLISHED, saved.getValue().getStatus());
        assertTrue(saved.getValue().getVerifiedPurchase());
    }

    @Test
    void verified_purchase_is_false_when_no_paid_order_exists() {
        stubProductExists();
        when(productReviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(false);
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(appUser(USER_ID, "Sara", "Ahmadi")));
        when(orderRepository.existsPaidOrderForProduct(USER_ID, PRODUCT_ID)).thenReturn(false);

        ReviewResponseDto response = service.create(USER_ID, PRODUCT_ID, request(4, null, null));

        assertFalse(response.getVerifiedPurchase());
        assertEquals("Sara Ahmadi", response.getAuthorName());
    }

    @Test
    void unknown_product_throws_product_not_found() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(false);

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> service.create(USER_ID, PRODUCT_ID, request(5, "x", "y")));

        assertEquals(ECOMErrorType.PRODUCT_NOT_FOUND, exception.getEcomErrorType());
        verify(productReviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void second_review_by_same_user_throws_already_exists() {
        stubProductExists();
        when(productReviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(true);

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> service.create(USER_ID, PRODUCT_ID, request(5, "x", "y")));

        assertEquals(ECOMErrorType.PRODUCT_REVIEW_ALREADY_EXISTS, exception.getEcomErrorType());
        verify(productReviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void concurrent_duplicate_insert_maps_unique_violation_to_already_exists() {
        stubProductExists();
        when(productReviewRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(false);
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(appUser(USER_ID, "Amir", "Zaman")));
        when(orderRepository.existsPaidOrderForProduct(USER_ID, PRODUCT_ID)).thenReturn(false);
        when(productReviewRepository.saveAndFlush(any(ProductReview.class)))
                .thenThrow(new DataIntegrityViolationException("uk_product_review_user_product"));

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> service.create(USER_ID, PRODUCT_ID, request(5, "x", "y")));

        assertEquals(ECOMErrorType.PRODUCT_REVIEW_ALREADY_EXISTS, exception.getEcomErrorType());
    }
}
