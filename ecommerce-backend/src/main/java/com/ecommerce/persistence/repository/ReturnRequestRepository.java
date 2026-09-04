package com.ecommerce.persistence.repository;

import com.ecommerce.persistence.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    List<ReturnRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ReturnRequest> findByIdAndUserId(Long id, Long userId);

    boolean existsByOrderId(Long orderId);
}
