package com.ecommerce.persistence.repository;

import com.ecommerce.persistence.entity.MockOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MockOtpRepository extends JpaRepository<MockOtp, Long> {

    Optional<MockOtp> findFirstByOrderByIdAsc();
}
