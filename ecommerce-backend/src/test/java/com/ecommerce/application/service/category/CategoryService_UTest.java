package com.ecommerce.application.service.category;

import com.ecommerce.application.api.dto.category.CreateCategoryRequestDto;
import com.ecommerce.application.api.dto.category.UpdateCategoryRequestDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.Category;
import com.ecommerce.persistence.repository.CategoryRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryService_UTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    private CategoryService service;

    private static Category category(Long id, String name, String localName, Long parentId) {
        var c = new Category();
        c.setId(id);
        c.setName(name);
        c.setLocalName(localName);
        c.setParentId(parentId);
        return c;
    }

    private static CreateCategoryRequestDto createDto(String name, String localName) {
        var dto = new CreateCategoryRequestDto();
        dto.setName(name);
        dto.setLocalName(localName);
        return dto;
    }

    private static UpdateCategoryRequestDto updateDto(String name, String localName, Long parentId) {
        var dto = new UpdateCategoryRequestDto();
        dto.setName(name);
        dto.setLocalName(localName);
        dto.setParentId(parentId);
        return dto;
    }

    @BeforeEach
    void setUp() {
        var mapper = new CategoryMapperImpl();
        service = new CategoryService(categoryRepository, productRepository, mapper);
    }

    // ---------------------------------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------------------------------

    @Test
    void get_all_returns_all_categories() {
        when(categoryRepository.findAll()).thenReturn(List.of(
                category(1L, "Electronics", null, null),
                category(2L, "Clothing", null, null)));

        var result = service.getAll();

        assertEquals(2, result.getCategories().size());
        assertEquals("Electronics", result.getCategories().get(0).getName());
        assertEquals("Clothing", result.getCategories().get(1).getName());
    }

    @Test
    void get_hierarchy_returns_roots_with_subcategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(
                category(1L, "Electronics", null, null),
                category(2L, "Mobile", null, 1L),
                category(3L, "Laptop", null, 1L),
                category(4L, "Clothing", null, null)));

        var result = service.getHierarchy();

        assertEquals(2, result.getCategories().size());
        var electronics = result.getCategories().get(0);
        assertEquals("Electronics", electronics.getCategory().getName());
        assertEquals(2, electronics.getSubCategories().size());
        assertEquals("Mobile", electronics.getSubCategories().get(0).getName());
        assertEquals(0, result.getCategories().get(1).getSubCategories().size());
    }

    @Test
    void get_hierarchy_only_subcategories_returns_empty() {
        when(categoryRepository.findAll()).thenReturn(List.of(category(1L, "Mobile", null, 5L)));

        assertEquals(0, service.getHierarchy().getCategories().size());
    }

    @Test
    void get_by_id_returns_category() {
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category(1L, "Electronics", "لوازم الکترونیک", null)));

        var result = service.getById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Electronics", result.getName());
        assertEquals("لوازم الکترونیک", result.getLocalName());
        assertNull(result.getParentId());
    }

    @Test
    void get_by_id_not_found_throws() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        var ex = assertThrows(EcommerceException.class, () -> service.getById(99L));
        assertEquals(ECOMErrorType.CATEGORY_NOT_FOUND, ex.getEcomErrorType());
    }

    // ---------------------------------------------------------------------------------------------
    // Create root category
    // ---------------------------------------------------------------------------------------------

    @Test
    void create_saves_root_category() {
        var req = createDto("Books", "کتاب");
        when(categoryRepository.existsByNameAndParentIdIsNull("Books")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        var result = service.create(req);

        assertEquals(10L, result.getId());
        assertEquals("Books", result.getName());
        assertEquals("کتاب", result.getLocalName());
        assertNull(result.getParentId());
    }

    @Test
    void create_duplicate_root_name_throws() {
        var req = createDto("Electronics", null);
        when(categoryRepository.existsByNameAndParentIdIsNull("Electronics")).thenReturn(true);

        var ex = assertThrows(EcommerceException.class, () -> service.create(req));
        assertEquals(ECOMErrorType.CATEGORY_NAME_ALREADY_EXISTS, ex.getEcomErrorType());
        verify(categoryRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------------------------------
    // Create sub-category
    // ---------------------------------------------------------------------------------------------

    @Test
    void create_subcategory_saves_under_root() {
        var req = createDto("Mobile", "موبایل");
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category(5L, "Electronics", null, null)));
        when(categoryRepository.existsByNameAndParentId("Mobile", 5L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        var result = service.createSubCategory(5L, req);

        assertEquals(10L, result.getId());
        assertEquals("Mobile", result.getName());
        assertEquals(5L, result.getParentId());
    }

    @Test
    void create_subcategory_parent_not_found_throws() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        var ex = assertThrows(EcommerceException.class,
                () -> service.createSubCategory(99L, createDto("Mobile", null)));
        assertEquals(ECOMErrorType.CATEGORY_NOT_FOUND, ex.getEcomErrorType());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_subcategory_under_subcategory_throws_depth() {
        // parent is itself a sub-category (parentId = 1) -> nesting a third level is rejected
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category(5L, "Mobile", null, 1L)));

        var ex = assertThrows(EcommerceException.class,
                () -> service.createSubCategory(5L, createDto("Android", null)));
        assertEquals(ECOMErrorType.CATEGORY_MAX_DEPTH_EXCEEDED, ex.getEcomErrorType());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_subcategory_duplicate_sibling_name_throws() {
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category(5L, "Electronics", null, null)));
        when(categoryRepository.existsByNameAndParentId("Mobile", 5L)).thenReturn(true);

        var ex = assertThrows(EcommerceException.class,
                () -> service.createSubCategory(5L, createDto("Mobile", null)));
        assertEquals(ECOMErrorType.CATEGORY_NAME_ALREADY_EXISTS, ex.getEcomErrorType());
        verify(categoryRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------------------------------------

    @Test
    void update_changes_name_and_local_name() {
        var existing = category(5L, "Old", "قدیم", null);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameAndParentIdIsNullAndIdNot("New Name", 5L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(existing);

        var result = service.update(5L, updateDto("New Name", "جدید", null));

        assertEquals("New Name", result.getName());
        assertEquals("جدید", result.getLocalName());
    }

    @Test
    void update_not_found_throws() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        var ex = assertThrows(EcommerceException.class,
                () -> service.update(99L, updateDto("Anything", null, null)));
        assertEquals(ECOMErrorType.CATEGORY_NOT_FOUND, ex.getEcomErrorType());
    }

    @Test
    void update_duplicate_root_name_throws() {
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category(5L, "Electronics", null, null)));
        when(categoryRepository.existsByNameAndParentIdIsNullAndIdNot("Clothing", 5L)).thenReturn(true);

        var ex = assertThrows(EcommerceException.class,
                () -> service.update(5L, updateDto("Clothing", null, null)));
        assertEquals(ECOMErrorType.CATEGORY_NAME_ALREADY_EXISTS, ex.getEcomErrorType());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_self_parent_throws_invalid_parent() {
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category(5L, "Electronics", null, null)));

        var ex = assertThrows(EcommerceException.class,
                () -> service.update(5L, updateDto("Electronics", null, 5L)));
        assertEquals(ECOMErrorType.CATEGORY_INVALID_PARENT, ex.getEcomErrorType());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_parent_not_found_throws() {
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category(5L, "Electronics", null, null)));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        var ex = assertThrows(EcommerceException.class,
                () -> service.update(5L, updateDto("Electronics", null, 99L)));
        assertEquals(ECOMErrorType.CATEGORY_NOT_FOUND, ex.getEcomErrorType());
    }

    @Test
    void update_parent_is_subcategory_throws_depth() {
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category(5L, "Electronics", null, null)));
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(category(7L, "Mobile", null, 1L)));

        var ex = assertThrows(EcommerceException.class,
                () -> service.update(5L, updateDto("Electronics", null, 7L)));
        assertEquals(ECOMErrorType.CATEGORY_MAX_DEPTH_EXCEEDED, ex.getEcomErrorType());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_category_with_children_cannot_become_sub_throws_depth() {
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category(5L, "Electronics", null, null)));
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(category(7L, "Clothing", null, null)));
        when(categoryRepository.existsByParentId(5L)).thenReturn(true);

        var ex = assertThrows(EcommerceException.class,
                () -> service.update(5L, updateDto("Electronics", null, 7L)));
        assertEquals(ECOMErrorType.CATEGORY_MAX_DEPTH_EXCEEDED, ex.getEcomErrorType());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_moves_leaf_under_new_root_succeeds() {
        var existing = category(5L, "Electronics", null, null);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(category(7L, "Clothing", null, null)));
        when(categoryRepository.existsByParentId(5L)).thenReturn(false);
        when(categoryRepository.existsByNameAndParentIdAndIdNot("Electronics", 7L, 5L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(existing);

        var result = service.update(5L, updateDto("Electronics", null, 7L));

        assertEquals(7L, result.getParentId());
    }

    // ---------------------------------------------------------------------------------------------
    // Delete
    // ---------------------------------------------------------------------------------------------

    @Test
    void delete_removes_leaf_category() {
        var cat = category(5L, "ToDelete", null, 1L);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(cat));
        when(categoryRepository.existsByParentId(5L)).thenReturn(false);
        when(productRepository.existsByCategoryIdOrSubCategoryId(5L, 5L)).thenReturn(false);

        service.delete(5L);

        verify(categoryRepository).delete(cat);
    }

    @Test
    void delete_not_found_throws() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        var ex = assertThrows(EcommerceException.class, () -> service.delete(99L));
        assertEquals(ECOMErrorType.CATEGORY_NOT_FOUND, ex.getEcomErrorType());
    }

    @Test
    void delete_with_subcategories_throws() {
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category(5L, "Electronics", null, null)));
        when(categoryRepository.existsByParentId(5L)).thenReturn(true);

        var ex = assertThrows(EcommerceException.class, () -> service.delete(5L));
        assertEquals(ECOMErrorType.CATEGORY_HAS_SUBCATEGORIES, ex.getEcomErrorType());
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_in_use_by_product_throws() {
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category(5L, "Electronics", null, null)));
        when(categoryRepository.existsByParentId(5L)).thenReturn(false);
        when(productRepository.existsByCategoryIdOrSubCategoryId(5L, 5L)).thenReturn(true);

        var ex = assertThrows(EcommerceException.class, () -> service.delete(5L));
        assertEquals(ECOMErrorType.CATEGORY_IN_USE, ex.getEcomErrorType());
        verify(categoryRepository, never()).delete(any());
    }
}
