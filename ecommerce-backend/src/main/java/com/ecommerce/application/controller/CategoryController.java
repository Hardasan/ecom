package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.category.*;
import com.ecommerce.application.service.category.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public CategoryListResponseDto getAll() {
        return categoryService.getAll();
    }

    @GetMapping(value = "/hierarchy", produces = MediaType.APPLICATION_JSON_VALUE)
    public CategoryHierarchyListResponseDto getHierarchy() {
        return categoryService.getHierarchy();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CategoryResponseDto getById(@PathVariable Long id) {
        return categoryService.getById(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponseDto create(@RequestBody CreateCategoryRequestDto requestDto) {
        return categoryService.create(requestDto);
    }

    @PostMapping(value = "/{parentId}/subcategories",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponseDto createSubCategory(@PathVariable Long parentId,
            @RequestBody CreateCategoryRequestDto requestDto) {
        return categoryService.createSubCategory(parentId, requestDto);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponseDto update(@PathVariable Long id,
            @RequestBody UpdateCategoryRequestDto requestDto) {
        return categoryService.update(id, requestDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
