package com.ecommerce.application.service.category;

import com.ecommerce.application.api.dto.category.CategoryResponseDto;
import com.ecommerce.application.api.dto.category.CreateCategoryRequestDto;
import com.ecommerce.application.api.dto.category.UpdateCategoryRequestDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.Category;
import com.ecommerce.persistence.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAll() {
        return categoryMapper.toResponseDtoList(categoryRepository.findAll());
    }

    @Transactional(readOnly = true)
    public CategoryResponseDto getById(Long id) {
        return categoryMapper.toResponseDto(findOrThrow(id));
    }

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
