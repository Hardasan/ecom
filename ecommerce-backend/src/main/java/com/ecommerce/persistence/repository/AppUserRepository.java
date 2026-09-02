package com.ecommerce.persistence.repository;

import com.ecommerce.persistence.entity.AppUser;
import com.ecommerce.persistence.entity.enumeration.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByMobile(String mobileNumber);

    boolean existsByMobile(String mobile);

    List<AppUser> findByRoleOrderByIdDesc(UserRole role);
}
