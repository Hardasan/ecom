package com.ecommerce.application.api.dto.review;

import com.ecommerce.persistence.entity.enumeration.ReviewStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ReviewResponseDto {

    private Long id;

    private Long productId;

    // The author's internal user id is deliberately not exposed: the list is public, and
    // authorName is the only author-facing field a client needs.
    private String authorName;

    private Integer rating;

    private String title;

    private String comment;

    private Boolean verifiedPurchase;

    private ReviewStatus status;

    private Date createdAt;

    private Date updatedAt;
}
