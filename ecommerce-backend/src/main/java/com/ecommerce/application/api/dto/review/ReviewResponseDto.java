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

    private Long userId;

    private String authorName;

    private Integer rating;

    private String title;

    private String comment;

    private Boolean verifiedPurchase;

    private ReviewStatus status;

    private Date createdAt;

    private Date updatedAt;
}
