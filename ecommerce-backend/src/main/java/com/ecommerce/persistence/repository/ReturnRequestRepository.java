package com.ecommerce.persistence.repository;

import com.ecommerce.persistence.entity.ReturnRequest;
import com.ecommerce.persistence.entity.enumeration.ReturnStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    List<ReturnRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ReturnRequest> findByIdAndUserId(Long id, Long userId);

    boolean existsByOrderId(Long orderId);

    // ---- admin moderation queue ------------------------------------------------------------------

    List<ReturnRequest> findAllByOrderByCreatedAtDesc();

    List<ReturnRequest> findByStatusOrderByCreatedAtDesc(ReturnStatus status);

    long countByStatus(ReturnStatus status);

    /**
     * Row-locked load for the approve/reject/refund transitions so two concurrent admin actions on
     * the same return serialize — the second sees the status the first left behind (mirrors the
     * order write path's {@code findByIdForUpdate}).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReturnRequest r WHERE r.id = :id")
    Optional<ReturnRequest> findByIdForUpdate(@Param("id") Long id);
}
