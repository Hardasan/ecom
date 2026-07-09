package com.ecommerce.persistence.repository;

import com.ecommerce.persistence.entity.CartItem;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import com.ecommerce.persistence.entity.enumeration.VariantValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserId(Long userId);

    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    /**
     * Locate a cart line by its full key, including null-safe equality on the variant columns so
     * variant-less products (variantType / variantValue both null) get their own line per user.
     */
    @Query("""
            SELECT ci FROM CartItem ci
            WHERE ci.userId = :userId
              AND ci.productId = :productId
              AND ((:variantType IS NULL AND ci.variantType IS NULL)
                   OR ci.variantType = :variantType)
              AND ((:variantValue IS NULL AND ci.variantValue IS NULL)
                   OR ci.variantValue = :variantValue)
            """)
    Optional<CartItem> findCartLine(@Param("userId") Long userId,
                                    @Param("productId") Long productId,
                                    @Param("variantType") VariantType variantType,
                                    @Param("variantValue") VariantValue variantValue);

    void deleteByUserId(Long userId);
}