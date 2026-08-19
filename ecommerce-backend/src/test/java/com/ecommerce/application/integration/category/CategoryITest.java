package com.ecommerce.application.integration.category;

import com.ecommerce.application.api.dto.category.CreateCategoryRequestDto;
import com.ecommerce.application.api.dto.category.UpdateCategoryRequestDto;
import com.ecommerce.application.integration.AbstractIntegrationITest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryITest extends AbstractIntegrationITest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private Long electronicsId;

    @BeforeEach
    void setupFixtures() throws Exception {
        jdbcTemplate.execute("TRUNCATE TABLE category RESTART IDENTITY CASCADE");

        String adminMobile = "09100000000";
        jdbcTemplate.update(
                "INSERT INTO app_user (first_name, last_name, username, mobile, password, role, is_enabled, is_registered) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "Admin", "User", adminMobile, adminMobile,
                passwordEncoder.encode("Admin123!"), "ROLE_ADMIN", true, true);
        adminToken = login(adminMobile, "Admin123!");

        String userMobile = newMobile();
        jdbcTemplate.update(
                "INSERT INTO app_user (first_name, last_name, username, mobile, password, role, is_enabled, is_registered) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "Test", "User", userMobile, userMobile,
                passwordEncoder.encode(DEFAULT_PASSWORD), "ROLE_APP_USER", true, true);
        userToken = login(userMobile, DEFAULT_PASSWORD);

        electronicsId = jdbcTemplate.queryForObject(
                "INSERT INTO category (name, local_name) VALUES ('Electronics', 'الکترونیک') RETURNING id", Long.class);
    }

    private Long insertSubCategory(String name, Long parentId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO category (name, parent_id) VALUES (?, ?) RETURNING id", Long.class, name, parentId);
    }

    // ---------------------------------------------------------------------------------------------
    // GET — public read
    // ---------------------------------------------------------------------------------------------

    @Test
    void get_all_returns_all_categories() throws Exception {
        insertSubCategory("Clothing", electronicsId);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(2)))
                .andExpect(jsonPath("$.categories[0].name").value("Electronics"))
                .andExpect(jsonPath("$.categories[0].localName").value("الکترونیک"))
                .andExpect(jsonPath("$.categories[1].name").value("Clothing"))
                .andExpect(jsonPath("$.categories[1].parentId").value(electronicsId));
    }

    @Test
    void get_all_no_auth_required() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(1)));
    }

    @Test
    void get_hierarchy_returns_roots_with_subcategories() throws Exception {
        insertSubCategory("Mobile", electronicsId);
        insertSubCategory("Laptop", electronicsId);
        jdbcTemplate.update("INSERT INTO category (name) VALUES ('Clothing')");

        mockMvc.perform(get("/api/categories/hierarchy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(2)))
                .andExpect(jsonPath("$.categories[0].category.name").value("Electronics"))
                .andExpect(jsonPath("$.categories[0].subCategories", hasSize(2)))
                .andExpect(jsonPath("$.categories[0].subCategories[0].name").value("Mobile"))
                .andExpect(jsonPath("$.categories[0].subCategories[1].name").value("Laptop"))
                .andExpect(jsonPath("$.categories[1].category.name").value("Clothing"))
                .andExpect(jsonPath("$.categories[1].subCategories", hasSize(0)));
    }

    @Test
    void get_by_id_returns_category() throws Exception {
        mockMvc.perform(get("/api/categories/{id}", electronicsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(electronicsId))
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.localName").value("الکترونیک"))
                .andExpect(jsonPath("$.parentId", nullValue()));
    }

    @Test
    void get_by_id_not_found_returns_404() throws Exception {
        mockMvc.perform(get("/api/categories/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NOT_FOUND"));
    }

    // ---------------------------------------------------------------------------------------------
    // POST root category — admin only
    // ---------------------------------------------------------------------------------------------

    @Test
    void create_saves_and_returns_root_category() throws Exception {
        var req = new CreateCategoryRequestDto();
        req.setName("Books");
        req.setLocalName("کتاب");

        MvcResult result = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Books"))
                .andExpect(jsonPath("$.localName").value("کتاب"))
                .andExpect(jsonPath("$.parentId", nullValue()))
                .andReturn();

        Long newId = json(result).get("id").asLong();
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM category WHERE id = ?", newId);
        assertEquals("Books", row.get("name"));
        assertNull(row.get("parent_id"));
    }

    @Test
    void create_duplicate_root_name_returns_conflict_and_db_unchanged() throws Exception {
        var req = new CreateCategoryRequestDto();
        req.setName("Electronics");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NAME_ALREADY_EXISTS"));

        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM category", Integer.class));
    }

    @Test
    void create_missing_name_returns_400_and_db_unchanged() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCategoryRequestDto())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM category", Integer.class));
    }

    @Test
    void create_requires_admin() throws Exception {
        var req = new CreateCategoryRequestDto();
        req.setName("Test");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM category", Integer.class));
    }

    @Test
    void create_without_auth_returns_401() throws Exception {
        var req = new CreateCategoryRequestDto();
        req.setName("Test");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------------------------------------
    // POST sub-category — admin only
    // ---------------------------------------------------------------------------------------------

    @Test
    void create_subcategory_saves_under_root() throws Exception {
        var req = new CreateCategoryRequestDto();
        req.setName("Mobile");
        req.setLocalName("موبایل");

        MvcResult result = mockMvc.perform(post("/api/categories/{parentId}/subcategories", electronicsId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mobile"))
                .andExpect(jsonPath("$.localName").value("موبایل"))
                .andExpect(jsonPath("$.parentId").value(electronicsId))
                .andReturn();

        Long newId = json(result).get("id").asLong();
        assertEquals(electronicsId, jdbcTemplate.queryForObject(
                "SELECT parent_id FROM category WHERE id = ?", Long.class, newId));
    }

    @Test
    void create_subcategory_same_name_under_different_parents_is_allowed() throws Exception {
        Long clothingId = jdbcTemplate.queryForObject(
                "INSERT INTO category (name) VALUES ('Clothing') RETURNING id", Long.class);

        var req = new CreateCategoryRequestDto();
        req.setName("Accessories");

        mockMvc.perform(post("/api/categories/{parentId}/subcategories", electronicsId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/categories/{parentId}/subcategories", clothingId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category WHERE name = 'Accessories'", Integer.class));
    }

    @Test
    void create_subcategory_duplicate_sibling_name_returns_conflict() throws Exception {
        insertSubCategory("Mobile", electronicsId);

        var req = new CreateCategoryRequestDto();
        req.setName("Mobile");

        mockMvc.perform(post("/api/categories/{parentId}/subcategories", electronicsId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NAME_ALREADY_EXISTS"));
    }

    @Test
    void create_subcategory_under_subcategory_returns_400() throws Exception {
        Long mobileId = insertSubCategory("Mobile", electronicsId);

        var req = new CreateCategoryRequestDto();
        req.setName("Android");

        mockMvc.perform(post("/api/categories/{parentId}/subcategories", mobileId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_MAX_DEPTH_EXCEEDED"));
    }

    @Test
    void create_subcategory_parent_not_found_returns_404() throws Exception {
        var req = new CreateCategoryRequestDto();
        req.setName("Orphan");

        mockMvc.perform(post("/api/categories/{parentId}/subcategories", 9999)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void create_subcategory_requires_admin() throws Exception {
        var req = new CreateCategoryRequestDto();
        req.setName("Mobile");

        mockMvc.perform(post("/api/categories/{parentId}/subcategories", electronicsId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM category", Integer.class));
    }

    // ---------------------------------------------------------------------------------------------
    // PUT — admin only
    // ---------------------------------------------------------------------------------------------

    @Test
    void update_changes_all_fields() throws Exception {
        var req = new UpdateCategoryRequestDto();
        req.setName("Updated Electronics");
        req.setLocalName("الکترونیک جدید");

        mockMvc.perform(put("/api/categories/{id}", electronicsId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Electronics"))
                .andExpect(jsonPath("$.localName").value("الکترونیک جدید"));
    }

    @Test
    void update_sets_parent_id() throws Exception {
        Long clothingId = jdbcTemplate.queryForObject(
                "INSERT INTO category (name) VALUES ('Clothing') RETURNING id", Long.class);

        var req = new UpdateCategoryRequestDto();
        req.setName("Electronics");
        req.setParentId(clothingId);

        mockMvc.perform(put("/api/categories/{id}", electronicsId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(clothingId));

        assertEquals(clothingId, jdbcTemplate.queryForObject(
                "SELECT parent_id FROM category WHERE id = ?", Long.class, electronicsId));
    }

    @Test
    void update_self_parent_returns_400() throws Exception {
        var req = new UpdateCategoryRequestDto();
        req.setName("Electronics");
        req.setParentId(electronicsId);

        mockMvc.perform(put("/api/categories/{id}", electronicsId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_INVALID_PARENT"));

        assertNull(jdbcTemplate.queryForObject(
                "SELECT parent_id FROM category WHERE id = ?", Long.class, electronicsId));
    }

    @Test
    void update_move_category_with_children_returns_400() throws Exception {
        insertSubCategory("Mobile", electronicsId);
        Long clothingId = jdbcTemplate.queryForObject(
                "INSERT INTO category (name) VALUES ('Clothing') RETURNING id", Long.class);

        var req = new UpdateCategoryRequestDto();
        req.setName("Electronics");
        req.setParentId(clothingId);

        mockMvc.perform(put("/api/categories/{id}", electronicsId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_MAX_DEPTH_EXCEEDED"));
    }

    @Test
    void update_duplicate_name_returns_conflict() throws Exception {
        jdbcTemplate.update("INSERT INTO category (name) VALUES ('Clothing')");

        var req = new UpdateCategoryRequestDto();
        req.setName("Clothing");

        mockMvc.perform(put("/api/categories/{id}", electronicsId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NAME_ALREADY_EXISTS"));

        assertEquals("Electronics", jdbcTemplate.queryForObject(
                "SELECT name FROM category WHERE id = ?", String.class, electronicsId));
    }

    @Test
    void update_requires_admin_and_db_unchanged() throws Exception {
        var req = new UpdateCategoryRequestDto();
        req.setName("Hacked");

        mockMvc.perform(put("/api/categories/{id}", electronicsId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        assertEquals("Electronics", jdbcTemplate.queryForObject(
                "SELECT name FROM category WHERE id = ?", String.class, electronicsId));
    }

    // ---------------------------------------------------------------------------------------------
    // DELETE — admin only
    // ---------------------------------------------------------------------------------------------

    @Test
    void delete_removes_leaf_category() throws Exception {
        Long toDelete = insertSubCategory("ToDelete", electronicsId);

        mockMvc.perform(delete("/api/categories/{id}", toDelete)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category WHERE id = ?", Integer.class, toDelete));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM category", Integer.class));
    }

    @Test
    void delete_with_subcategories_returns_conflict() throws Exception {
        insertSubCategory("Mobile", electronicsId);

        mockMvc.perform(delete("/api/categories/{id}", electronicsId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_HAS_SUBCATEGORIES"));

        assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM category", Integer.class));
    }

    @Test
    void delete_in_use_by_product_returns_conflict() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO product (code, category_id, url, name, inventory_status, status) VALUES (?, ?, ?, ?, ?, ?)",
                "TEST-CODE", electronicsId, "test-product-url", "Test Product", "IN_STOCK", "ACTIVE");

        mockMvc.perform(delete("/api/categories/{id}", electronicsId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_IN_USE"));

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category WHERE id = ?", Integer.class, electronicsId));
    }

    @Test
    void delete_not_found_returns_404() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", 9999)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void delete_requires_admin_and_db_unchanged() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", electronicsId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM category", Integer.class));
    }

    @Test
    void delete_without_auth_returns_401() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", electronicsId))
                .andExpect(status().isUnauthorized());
    }
}
