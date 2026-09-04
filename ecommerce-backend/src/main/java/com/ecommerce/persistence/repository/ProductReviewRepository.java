package com.ecommerce.persistence.repository;

import com.ecommerce.application.api.dto.review.AdminReviewResponseDto;
import com.ecommerce.persistence.entity.ProductReview;
import com.ecommerce.persistence.entity.enumeration.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository
        extends JpaRepository<ProductReview, Long>, JpaSpecificationExecutor<ProductReview> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<ProductReview> findByIdAndProductId(Long id, Long productId);

    Optional<ProductReview> findByIdAndProductIdAndUserId(Long id, Long productId, Long userId);

    /**
     * Star histogram over the PUBLISHED reviews of a product: one {@code [rating, count]} row per
     * distinct rating present. The average and total are derived from this in the service, so the
     * summary needs a single query.
     */
    @Query("SELECT r.rating, COUNT(r) FROM ProductReview r "
            + "WHERE r.productId = :productId AND r.status = 'PUBLISHED' GROUP BY r.rating")
    List<Object[]> countPublishedGroupedByRating(@Param("productId") Long productId);

    /**
     * Batch rating aggregate for product cards/lists: one {@code [productId, avgRating, count]} row
     * per product that has PUBLISHED reviews. Absent products simply have no row (count 0). One
     * query enriches a whole page of products, so the card can show «۴٫۸ (۲۱۴)» without N+1 lookups.
     */
    @Query("SELECT r.productId, AVG(r.rating), COUNT(r) FROM ProductReview r "
            + "WHERE r.productId IN :productIds AND r.status = 'PUBLISHED' GROUP BY r.productId")
    List<Object[]> aggregatePublishedByProductIds(@Param("productIds") Collection<Long> productIds);

    long countByStatus(ReviewStatus status);

    /**
     * Cross-product moderation queue: every review joined to its product for name/code, ordered
     * newest-first. The product is matched on the snapshotted {@code product_id} (reviews hold an id,
     * not a mapped association). The status filter is split into two methods (rather than a nullable
     * {@code :status} parameter) so neither query ever binds a null enum.
     */
    String ADMIN_REVIEW_SELECT = """
            SELECT new com.ecommerce.application.api.dto.review.AdminReviewResponseDto(
                r.id, r.productId, p.name, p.localName, p.code, r.authorName, r.rating,
                r.title, r.comment, r.verifiedPurchase, r.status, r.createdAt, r.updatedAt)
            FROM ProductReview r, Product p
            WHERE r.productId = p.id""";

    String ADMIN_REVIEW_ORDER = " ORDER BY r.createdAt DESC, r.id DESC";

    @Query(value = ADMIN_REVIEW_SELECT + ADMIN_REVIEW_ORDER,
            countQuery = "SELECT COUNT(r) FROM ProductReview r")
    Page<AdminReviewResponseDto> findAllAdminReviews(Pageable pageable);

    @Query(value = ADMIN_REVIEW_SELECT + " AND r.status = :status" + ADMIN_REVIEW_ORDER,
            countQuery = "SELECT COUNT(r) FROM ProductReview r WHERE r.status = :status")
    Page<AdminReviewResponseDto> findAdminReviewsByStatus(@Param("status") ReviewStatus status, Pageable pageable);
}
