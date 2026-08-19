package com.ecommerce.application.service.category;

import com.ecommerce.application.api.dto.category.CategoryResponseDto;
import com.ecommerce.application.api.dto.category.CreateCategoryRequestDto;
import com.ecommerce.application.api.dto.category.UpdateCategoryRequestDto;
import com.ecommerce.persistence.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentId", ignore = true)
    void apply(CreateCategoryRequestDto requestDto, @MappingTarget Category category);

    @Mapping(target = "id", ignore = true)
    void apply(UpdateCategoryRequestDto requestDto, @MappingTarget Category category);

    CategoryResponseDto toResponseDto(Category category);

    List<CategoryResponseDto> toResponseDtoList(List<Category> categories);
}
