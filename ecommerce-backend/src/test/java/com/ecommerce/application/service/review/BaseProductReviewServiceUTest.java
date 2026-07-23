package com.ecommerce.application.service.review;

import com.ecommerce.application.api.dto.review.ReviewRequestDto;
import com.ecommerce.application.api.dto.review.SearchReviewRequestDto;
import com.ecommerce.application.api.dto.review.enumeration.ReviewSort;
import com.ecommerce.persistence.entity.AppUser;
import com.ecommerce.persistence.entity.ProductReview;
import com.ecommerce.persistence.entity.enumeration.ReviewStatus;
import com.ecommerce.persistence.repository.AppUserRepository;
import com.ecommerce.persistence.repository.OrderRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import com.ecommerce.persistence.repository.ProductReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
abstract class BaseProductReviewServiceUTest {

    protected static final Long USER_ID = 7L;
    protected static final Long OTHER_USER_ID = 8L;
    protected static final Long PRODUCT_ID = 100L;
    protected static final Long REVIEW_ID = 500L;

    @Mock
    protected ProductReviewRepository productReviewRepository;
    @Mock
    protected ProductRepository productRepository;
    @Mock
    protected AppUserRepository appUserRepository;
    @Mock
    protected OrderRepository orderRepository;

    protected ProductReviewService service;

    @BeforeEach
    void baseSetUp() {
        service = new ProductReviewService(productReviewRepository, productRepository,
                appUserRepository, orderRepository, new ProductReviewMapperImpl());
        // save/saveAndFlush return the persisted instance; harmless if a given test never saves.
        lenient().when(productReviewRepository.save(any(ProductReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(productReviewRepository.saveAndFlush(any(ProductReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    protected ReviewRequestDto request(int rating, String title, String comment) {
        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setRating(rating);
        dto.setTitle(title);
        dto.setComment(comment);
        return dto;
    }

    protected SearchReviewRequestDto searchDto(ReviewSort sort, Integer rating, Boolean verifiedOnly) {
        SearchReviewRequestDto dto = new SearchReviewRequestDto();
        dto.setSort(sort);
        dto.setRating(rating);
        dto.setVerifiedOnly(verifiedOnly);
        return dto;
    }

    protected AppUser appUser(Long id, String firstName, String lastName) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }

    protected ProductReview review(Long id, Long productId, Long userId, int rating, ReviewStatus status,
                                   boolean verified) {
        ProductReview review = new ProductReview();
        review.setId(id);
        review.setProductId(productId);
        review.setUserId(userId);
        review.setRating(rating);
        review.setTitle("Original title");
        review.setComment("Original comment");
        review.setStatus(status);
        review.setVerifiedPurchase(verified);
        review.setAuthorName("Amir Zaman");
        return review;
    }

    protected void stubProductExists() {
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
    }
}
