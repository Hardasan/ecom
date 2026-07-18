package com.ecommerce.application.service.review;

import com.ecommerce.application.api.dto.review.ReviewResponseDto;
import com.ecommerce.application.api.dto.review.enumeration.ReviewSort;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.ProductReview;
import com.ecommerce.persistence.entity.enumeration.ReviewStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductReviewService_getReviewsUTest extends BaseProductReviewServiceUTest {

    @SuppressWarnings("unchecked")
    private void stubFindAll(ProductReview... reviews) {
        when(productReviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reviews)));
    }

    @Test
    void maps_results_and_applies_newest_sort_by_default() {
        stubProductExists();
        stubFindAll(
                review(1L, PRODUCT_ID, USER_ID, 5, ReviewStatus.PUBLISHED, true),
                review(2L, PRODUCT_ID, OTHER_USER_ID, 3, ReviewStatus.PUBLISHED, false));

        Page<ReviewResponseDto> page = service.getReviews(PRODUCT_ID, false,
                searchDto(ReviewSort.NEWEST, null, null), PageRequest.of(0, 20));

        assertEquals(2, page.getContent().size());
        assertEquals(5, page.getContent().get(0).getRating());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(productReviewRepository).findAll(any(Specification.class), pageable.capture());
        Sort.Order primary = pageable.getValue().getSort().iterator().next();
        assertEquals("createdAt", primary.getProperty());
        assertEquals(Sort.Direction.DESC, primary.getDirection());
    }

    @Test
    void highest_sort_orders_by_rating_descending() {
        stubProductExists();
        stubFindAll(review(1L, PRODUCT_ID, USER_ID, 5, ReviewStatus.PUBLISHED, true));

        service.getReviews(PRODUCT_ID, false, searchDto(ReviewSort.HIGHEST, null, null), PageRequest.of(0, 20));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(productReviewRepository).findAll(any(Specification.class), pageable.capture());
        Sort.Order primary = pageable.getValue().getSort().iterator().next();
        assertEquals("rating", primary.getProperty());
        assertEquals(Sort.Direction.DESC, primary.getDirection());
    }

    @Test
    void unknown_product_throws_product_not_found() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(false);

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> service.getReviews(PRODUCT_ID, false, searchDto(ReviewSort.NEWEST, null, null),
                        PageRequest.of(0, 20)));

        assertEquals(ECOMErrorType.PRODUCT_NOT_FOUND, exception.getEcomErrorType());
        verify(productReviewRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}
