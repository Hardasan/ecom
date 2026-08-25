package com.ecommerce.application.api.dto.review;

import com.ecommerce.persistence.entity.enumeration.ReviewStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * A review row for the cross-product admin moderation queue. Unlike the public
 * {@link ReviewResponseDto}, it carries the owning product's name/code so the queue can be read
 * without a per-row product lookup. Assembled directly by a JPQL constructor expression
 * (see {@code ProductReviewRepository.findAdminReviews}), so the constructor argument order is
 * significant and must match that query.
 */
@Getter
@Setter
public class AdminReviewResponseDto {

    private Long id;
    private Long productId;
    private String productName;
    private String productLocalName;
    private String productCode;
    private String authorName;
    private Integer rating;
    private String title;
    private String comment;
    private Boolean verifiedPurchase;
    private ReviewStatus status;
    private Date createdAt;
    private Date updatedAt;

    public AdminReviewResponseDto(Long id, Long productId, String productName, String productLocalName,
            String productCode, String authorName, Integer rating, String title, String comment,
            Boolean verifiedPurchase, ReviewStatus status, Date createdAt, Date updatedAt) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productLocalName = productLocalName;
        this.productCode = productCode;
        this.authorName = authorName;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.verifiedPurchase = verifiedPurchase;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
