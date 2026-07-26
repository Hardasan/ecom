package com.ecommerce.application.api.dto.review;

import com.ecommerce.application.api.dto.review.enumeration.ReviewSort;
import com.ecommerce.persistence.entity.enumeration.ReviewStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Filter/sort criteria for the review list, bound with {@code @ModelAttribute}. Page and size come
 * from the standard {@code Pageable}; ordering is driven by {@link #sort}, not raw Spring sort input.
 */
@Getter
@Setter
public class SearchReviewRequestDto {

    private ReviewSort sort = ReviewSort.NEWEST;

    @Min(1)
    @Max(5)
    private Integer rating;

    private Boolean verifiedOnly;

    // Admin-only moderation-queue filter (e.g. status=PENDING); ignored for non-admin callers,
    // who always see PUBLISHED only.
    private ReviewStatus status;
}
