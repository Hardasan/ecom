package com.ecommerce.persistence.repository;

import com.ecommerce.persistence.entity.Discount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {

    Optional<Discount> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    /**
     * Looks up a code and takes a pessimistic write lock (<code>SELECT … FOR UPDATE</code>) on its row.
     *
     * <p>This is the serialization point for redemption: while a checkout holds the lock, no other
     * checkout of the same code can read or change {@code usage_count}, so the whole
     * check-limits → compute → increment → place-order sequence runs without interleaving. It closes
     * both the global-limit race and the per-user race (a concurrent redemption by the same user waits
     * here and then sees the first order already committed). The lock is held until the checkout
     * transaction commits. {@code :code} must already be upper-cased by the caller.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Discount d WHERE upper(d.code) = :code")
    Optional<Discount> findByCodeForUpdate(@Param("code") String code);

    /**
     * Releases one previously-claimed redemption slot (order cancelled or reservation expired).
     * Guarded by {@code usage_count > 0} so it can never drive the counter negative; the statement
     * takes the row lock, so it serializes with a concurrent {@link #findByCodeForUpdate redemption}.
     *
     * @return 1 when a slot was released, 0 when there was nothing to release
     */
    @Modifying
    @Query("UPDATE Discount d SET d.usageCount = d.usageCount - 1 "
            + "WHERE d.id = :id AND d.usageCount > 0")
    int releaseRedemption(@Param("id") Long id);
}
