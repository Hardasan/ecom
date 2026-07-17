package com.ecommerce.persistence.repository;

import com.ecommerce.persistence.entity.CartItem;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserId(Long userId);

    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    Optional<CartItem> findByUserIdAndProductIdAndVariantTypeAndVariantValue(Long userId, Long productId,
                                                                             VariantType variantType,
                                                                             String variantValue);

    void deleteByUserId(Long userId);
}