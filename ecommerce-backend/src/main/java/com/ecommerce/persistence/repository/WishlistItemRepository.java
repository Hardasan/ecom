package com.ecommerce.persistence.repository;

import com.ecommerce.persistence.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    /**
     * Adds a bookmark unless the user already has one for the product, as a single atomic statement.
     *
     * <p>A read-then-{@code save()} cannot express this: two concurrent adds of the same product both
     * observe "absent" and race into {@code uk_wishlist_item_user_product}, so the loser gets a
     * {@code DataIntegrityViolationException} (a 500) instead of the documented idempotent no-op.
     * Catching that violation is not an option either — it poisons the surrounding transaction, so the
     * read-back that renders the response would fail too. Letting PostgreSQL resolve the conflict keeps
     * the transaction clean and collapses the duplicate to a no-op.
     *
     * <p>{@code id} and {@code created_at} are left to their column defaults
     * ({@code NEXTVAL('wishlist_item_seq')} / {@code NOW()}), matching what Hibernate would have written.
     *
     * @return 1 when a bookmark was created, 0 when the user already had one
     */
    @Modifying
    @Query(value = """
            INSERT INTO wishlist_item (user_id, product_id)
            VALUES (:userId, :productId)
            ON CONFLICT ON CONSTRAINT uk_wishlist_item_user_product DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("productId") Long productId);

    List<WishlistItem> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    Optional<WishlistItem> findByIdAndUserId(Long id, Long userId);

    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserId(Long userId);
}
