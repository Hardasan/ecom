package com.ecommerce.application.service.review;

import com.ecommerce.application.api.dto.review.ReviewRequestDto;
import com.ecommerce.application.api.dto.review.ReviewResponseDto;
import com.ecommerce.application.api.dto.review.ReviewSummaryResponseDto;
import com.ecommerce.application.api.dto.review.SearchReviewRequestDto;
import com.ecommerce.application.api.dto.review.enumeration.ReviewSort;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.AppUser;
import com.ecommerce.persistence.entity.ProductReview;
import com.ecommerce.persistence.entity.enumeration.ReviewStatus;
import com.ecommerce.persistence.repository.AppUserRepository;
import com.ecommerce.persistence.repository.OrderRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import com.ecommerce.persistence.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final AppUserRepository appUserRepository;
    private final OrderRepository orderRepository;
    private final ProductReviewMapper productReviewMapper;

    @Transactional
    public ReviewResponseDto create(Long userId, Long productId, ReviewRequestDto requestDto) {
        requireProductExists(productId);
        if (productReviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new EcommerceException(ECOMErrorType.PRODUCT_REVIEW_ALREADY_EXISTS);
        }
        AppUser author = appUserRepository.findById(userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.USER_NOT_FOUND));

        ProductReview review = new ProductReview();
        productReviewMapper.apply(requestDto, review);
        review.setProductId(productId);
        review.setUserId(userId);
        review.setAuthorName(buildAuthorName(author));
        review.setStatus(ReviewStatus.PUBLISHED);
        review.setVerifiedPurchase(orderRepository.existsPaidOrderForProduct(userId, productId));

        try {
            // Two concurrent POSTs can both pass the exists() pre-check; the loser then violates
            // uk_product_review_user_product. Flush here (sequence ids defer the insert to commit,
            // which is outside this try) so the violation maps to the same business error.
            return productReviewMapper.toResponseDto(productReviewRepository.saveAndFlush(review));
        } catch (DataIntegrityViolationException e) {
            throw new EcommerceException(ECOMErrorType.PRODUCT_REVIEW_ALREADY_EXISTS);
        }
    }

    @Transactional
    public ReviewResponseDto update(Long userId, Long productId, Long reviewId, ReviewRequestDto requestDto) {
        ProductReview review = productReviewRepository
                .findByIdAndProductIdAndUserId(reviewId, productId, userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.PRODUCT_REVIEW_NOT_FOUND));
        // authorName / verifiedPurchase / status are intentionally left untouched on edit.
        productReviewMapper.apply(requestDto, review);
        return productReviewMapper.toResponseDto(productReviewRepository.save(review));
    }

    @Transactional
    public void delete(Long userId, boolean isAdmin, Long productId, Long reviewId) {
        ProductReview review = (isAdmin
                ? productReviewRepository.findByIdAndProductId(reviewId, productId)
                : productReviewRepository.findByIdAndProductIdAndUserId(reviewId, productId, userId))
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.PRODUCT_REVIEW_NOT_FOUND));
        productReviewRepository.delete(review);
    }

    @Transactional
    public ReviewResponseDto moderate(Long productId, Long reviewId, ReviewStatus status) {
        ProductReview review = productReviewRepository.findByIdAndProductId(reviewId, productId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.PRODUCT_REVIEW_NOT_FOUND));
        review.setStatus(status);
        return productReviewMapper.toResponseDto(productReviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getReviews(Long productId, boolean isAdmin,
                                              SearchReviewRequestDto searchDto, Pageable pageable) {
        requireProductExists(productId);
        Specification<ProductReview> spec = ReviewSpecifications.build(productId, isAdmin, searchDto);
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                toSort(searchDto.getSort()));
        return productReviewRepository.findAll(spec, sorted).map(productReviewMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponseDto getSummary(Long productId) {
        requireProductExists(productId);

        Map<Integer, Long> ratingCounts = new LinkedHashMap<>();
        for (int star = 1; star <= 5; star++) {
            ratingCounts.put(star, 0L);
        }
        long total = 0;
        long weightedSum = 0;
        for (Object[] row : productReviewRepository.countPublishedGroupedByRating(productId)) {
            int rating = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            ratingCounts.put(rating, count);
            total += count;
            weightedSum += (long) rating * count;
        }
        BigDecimal average = total == 0
                ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(weightedSum).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);

        ReviewSummaryResponseDto dto = new ReviewSummaryResponseDto();
        dto.setProductId(productId);
        dto.setAverageRating(average);
        dto.setTotalCount(total);
        dto.setRatingCounts(ratingCounts);
        return dto;
    }

    private void requireProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new EcommerceException(ECOMErrorType.PRODUCT_NOT_FOUND);
        }
    }

    private String buildAuthorName(AppUser user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private Sort toSort(ReviewSort sort) {
        ReviewSort effective = sort == null ? ReviewSort.NEWEST : sort;
        return switch (effective) {
            case NEWEST -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            case OLDEST -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
            case HIGHEST -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            case LOWEST -> Sort.by(Sort.Order.asc("rating"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        };
    }
}
