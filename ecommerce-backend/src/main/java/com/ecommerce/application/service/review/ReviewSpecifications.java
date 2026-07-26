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

    static Specification<ProductReview> build(Long productId, boolean adminView, SearchReviewRequestDto dto) {
        var rating = dto.getRating();
        var verifiedOnly = dto.getVerifiedOnly();
        var status = dto.getStatus();
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("productId"), productId));
            if (!adminView) {
                // The public only ever sees approved reviews; the status filter is admin-only.
                predicates.add(cb.equal(root.get("status"), ReviewStatus.PUBLISHED));
            } else if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
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
