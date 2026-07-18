package com.ecommerce.application.service.review;

import com.ecommerce.application.api.dto.review.SearchReviewRequestDto;
import com.ecommerce.persistence.entity.ProductReview;
import com.ecommerce.persistence.entity.enumeration.ReviewStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

final class ReviewSpecifications {

    private ReviewSpecifications() {
    }

    static Specification<ProductReview> build(Long productId, boolean includeHidden, SearchReviewRequestDto dto) {
        var rating = dto.getRating();
        var verifiedOnly = dto.getVerifiedOnly();
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("productId"), productId));
            if (!includeHidden) {
                predicates.add(cb.equal(root.get("status"), ReviewStatus.PUBLISHED));
            }
            if (rating != null) {
                predicates.add(cb.equal(root.get("rating"), rating));
            }
            if (Boolean.TRUE.equals(verifiedOnly)) {
                predicates.add(cb.isTrue(root.get("verifiedPurchase")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
