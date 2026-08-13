package com.ecommerce.persistence.repository;

import com.ecommerce.persistence.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameAndParentIdIsNull(String name);

    boolean existsByNameAndParentIdIsNullAndIdNot(String name, Long id);

    boolean existsByNameAndParentId(String name, Long parentId);

    boolean existsByNameAndParentIdAndIdNot(String name, Long parentId, Long id);

    boolean existsByParentId(Long parentId);
}
