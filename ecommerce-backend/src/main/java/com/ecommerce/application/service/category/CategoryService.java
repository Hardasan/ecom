package com.ecommerce.application.service.category;

import com.ecommerce.application.api.dto.category.*;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.Category;
import com.ecommerce.persistence.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
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

    @Transactional
    public CategoryResponseDto create(CreateCategoryRequestDto requestDto) {
        if (categoryRepository.existsByName(requestDto.getName())) {
            throw new EcommerceException(ECOMErrorType.CATEGORY_NAME_ALREADY_EXISTS);
        }
        if (requestDto.getParentId() != null && !categoryRepository.existsById(requestDto.getParentId())) {
            throw new EcommerceException(ECOMErrorType.CATEGORY_NOT_FOUND);
        }

        var category = new Category();
        categoryMapper.apply(requestDto, category);
        return categoryMapper.toResponseDto(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponseDto update(Long id, UpdateCategoryRequestDto requestDto) {
        var category = findOrThrow(id);

        if (categoryRepository.existsByNameAndIdNot(requestDto.getName(), id)) {
            throw new EcommerceException(ECOMErrorType.CATEGORY_NAME_ALREADY_EXISTS);
        }
        if (requestDto.getParentId() != null
                && !requestDto.getParentId().equals(id)
                && !categoryRepository.existsById(requestDto.getParentId())) {
            throw new EcommerceException(ECOMErrorType.CATEGORY_NOT_FOUND);
        }

        categoryMapper.apply(requestDto, category);
        return categoryMapper.toResponseDto(categoryRepository.save(category));
    }

    public void delete(Long id) {
        categoryRepository.delete(findOrThrow(id));
    }

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.CATEGORY_NOT_FOUND));
    }
}
