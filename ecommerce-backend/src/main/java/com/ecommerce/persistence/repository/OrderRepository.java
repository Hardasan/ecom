package com.ecommerce.persistence.repository;

import com.ecommerce.persistence.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByIdDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    List<Order> findAllByOrderByIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.userId = :userId")
    Optional<Order> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.status = 'RESERVED' AND o.reservedUntil < :now")
    List<Order> findExpiredReservations(@Param("now") Date now);

    /**
     * True when the user has a paid-or-later order containing the product — used to stamp a review
     * as a verified purchase. Matched on the snapshotted {@code product_id} of the order line.
     */
    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.items i "
            + "WHERE o.userId = :userId AND i.product.productId = :productId "
            + "AND o.status IN ('PAID', 'SENDING', 'RECEIVED')")
    boolean existsPaidOrderForProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}
