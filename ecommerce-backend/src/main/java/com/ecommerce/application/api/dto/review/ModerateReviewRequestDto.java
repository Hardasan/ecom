package com.ecommerce.application.api.dto.review;

import com.ecommerce.persistence.entity.enumeration.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin moderation body: flip a review between {@code PUBLISHED} and {@code HIDDEN}.
 */
@Getter
@Setter
public class ModerateReviewRequestDto {

    @NotNull
    private ReviewStatus status;
}
