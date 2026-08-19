package com.ecommerce.application.service.category;

import com.ecommerce.application.api.dto.category.*;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.Category;
import com.ecommerce.persistence.repository.CategoryRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public CategoryListResponseDto getAll() {
        return new CategoryListResponseDto(categoryMapper.toResponseDtoList(categoryRepository.findAll()));
    }

    @Transactional(readOnly = true)
    public CategoryResponseDto getById(Long id) {
        return categoryMapper.toResponseDto(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public CategoryHierarchyListResponseDto getHierarchy() {
        var allCategories = categoryRepository.findAll();

        var childrenByParentId = allCategories.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        var items = allCategories.stream()
                .filter(c -> c.getParentId() == null)
                .map(root -> {
                    var sub = childrenByParentId.getOrDefault(root.getId(), List.of())
                            .stream()
                            .map(categoryMapper::toResponseDto)
                            .toList();
                    return new CategoryHierarchyItemDto(categoryMapper.toResponseDto(root), sub);
                })
                .toList();

        return new CategoryHierarchyListResponseDto(items);
    }

    /**
     * Adds a top-level (root) category. Names are unique among roots.
     */
    @Transactional
    public CategoryResponseDto create(CreateCategoryRequestDto requestDto) {
        if (categoryRepository.existsByNameAndParentIdIsNull(requestDto.getName())) {
            throw new EcommerceException(ECOMErrorType.CATEGORY_NAME_ALREADY_EXISTS);
        }

        var category = new Category();
        categoryMapper.apply(requestDto, category);
        return categoryMapper.toResponseDto(categoryRepository.save(category));
    }

    /**
     * Adds a sub-category under an existing root. The tree is capped at two levels, so the parent
     * must itself be a root. Names are unique among that parent's children.
     */
    @Transactional
    public CategoryResponseDto createSubCategory(Long parentId, CreateCategoryRequestDto requestDto) {
        var parent = findOrThrow(parentId);
        if (parent.getParentId() != null) {
            throw new EcommerceException(ECOMErrorType.CATEGORY_MAX_DEPTH_EXCEEDED);
        }
        if (categoryRepository.existsByNameAndParentId(requestDto.getName(), parentId)) {
            throw new EcommerceException(ECOMErrorType.CATEGORY_NAME_ALREADY_EXISTS);
        }

        var category = new Category();
        categoryMapper.apply(requestDto, category);
        category.setParentId(parentId);
        return categoryMapper.toResponseDto(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponseDto update(Long id, UpdateCategoryRequestDto requestDto) {
        var category = findOrThrow(id);

        validateParentForUpdate(id, requestDto.getParentId());
        validateNameForUpdate(id, requestDto.getName(), requestDto.getParentId());

        categoryMapper.apply(requestDto, category);
        return categoryMapper.toResponseDto(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        var category = findOrThrow(id);
        if (categoryRepository.existsByParentId(id)) {
            throw new EcommerceException(ECOMErrorType.CATEGORY_HAS_SUBCATEGORIES);
        }
        if (productRepository.existsByCategoryIdOrSubCategoryId(id, id)) {
            throw new EcommerceException(ECOMErrorType.CATEGORY_IN_USE);
        }
        categoryRepository.delete(category);
    }

    private void validateParentForUpdate(Long id, Long parentId) {
        if (parentId == null) {
            return; // moving to / staying a root
        }
        if (parentId.equals(id)) {
            throw new EcommerceException(ECOMErrorType.CATEGORY_INVALID_PARENT);
        }
        var parent = findOrThrow(parentId);
        // Two-level cap: the parent must be a root, and this category must not already have children.
        if (parent.getParentId() != null || categoryRepository.existsByParentId(id)) {
            throw new EcommerceException(ECOMErrorType.CATEGORY_MAX_DEPTH_EXCEEDED);
        }
    }

    private void validateNameForUpdate(Long id, String name, Long parentId) {
        boolean duplicate = parentId == null
                ? categoryRepository.existsByNameAndParentIdIsNullAndIdNot(name, id)
                : categoryRepository.existsByNameAndParentIdAndIdNot(name, parentId, id);
        if (duplicate) {
            throw new EcommerceException(ECOMErrorType.CATEGORY_NAME_ALREADY_EXISTS);
        }
    }

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.CATEGORY_NOT_FOUND));
    }
}
