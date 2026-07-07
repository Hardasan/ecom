package com.ecommerce.persistence.repository;

import com.ecommerce.persistence.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsByUrl(String url);

    boolean existsByUrlAndIdNot(String url, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Product p SET p.inventoryCount = p.inventoryCount - :quantity WHERE p.id = :id AND p.inventoryCount >= :quantity")
    int decrementInventory(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE Product p SET p.inventoryCount = p.inventoryCount + :quantity WHERE p.id = :id")
    int incrementInventory(@Param("id") Long id, @Param("quantity") int quantity);

    @Query("SELECT p.url FROM Product p")
    List<String> findAllUrls();
}
