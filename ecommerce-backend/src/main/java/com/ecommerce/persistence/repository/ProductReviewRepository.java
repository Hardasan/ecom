package com.ecommerce.persistence.repository;

import com.ecommerce.persistence.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
