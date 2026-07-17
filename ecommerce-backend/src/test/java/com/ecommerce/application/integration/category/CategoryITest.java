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

    // ---------------------------------------------------------------------------------------------
    // GET — public read
    // ---------------------------------------------------------------------------------------------

    @Test
    void get_all_returns_all_categories() throws Exception {
        jdbcTemplate.update("INSERT INTO category (name, local_name, parent_id) VALUES (?, ?, ?)",
                "Clothing", "لباس", electronicsId);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(2)))
                .andExpect(jsonPath("$.categories[0].name").value("Electronics"))
                .andExpect(jsonPath("$.categories[0].localName").value("الکترونیک"))
                .andExpect(jsonPath("$.categories[1].name").value("Clothing"))
                .andExpect(jsonPath("$.categories[1].localName").value("لباس"))
                .andExpect(jsonPath("$.categories[1].parentId").value(electronicsId));
    }

    @Test
    void get_all_no_auth_required() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(1)));

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category", Integer.class));
    }

    // ---------------------------------------------------------------------------------------------
    // GET hierarchy — public read
    // ---------------------------------------------------------------------------------------------

    @Test
    void get_hierarchy_returns_roots_with_subcategories() throws Exception {
        Long mobileId = jdbcTemplate.queryForObject(
                "INSERT INTO CATEGORY (NAME, LOCAL_NAME, PARENT_ID) VALUES (?, ?, ?) RETURNING ID",
                Long.class, "Mobile", "موبایل", electronicsId);
        Long laptopId = jdbcTemplate.queryForObject(
                "INSERT INTO CATEGORY (NAME, LOCAL_NAME, PARENT_ID) VALUES (?, ?, ?) RETURNING ID",
                Long.class, "Laptop", "لپتاپ", electronicsId);
        Long clothingId = jdbcTemplate.queryForObject(
                "INSERT INTO CATEGORY (NAME, LOCAL_NAME) VALUES (?, ?) RETURNING ID",
                Long.class, "Clothing", "لباس");

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
    void get_hierarchy_no_subcategories_returns_root_with_empty_array() throws Exception {
        mockMvc.perform(get("/api/categories/hierarchy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(1)))
                .andExpect(jsonPath("$.categories[0].category.name").value("Electronics"))
                .andExpect(jsonPath("$.categories[0].subCategories", hasSize(0)));
    }

    @Test
    void get_hierarchy_empty_returns_empty_list() throws Exception {
        jdbcTemplate.execute("TRUNCATE TABLE CATEGORY RESTART IDENTITY CASCADE");

        mockMvc.perform(get("/api/categories/hierarchy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(0)));
    }

    @Test
    void get_hierarchy_no_auth_required() throws Exception {
        mockMvc.perform(get("/api/categories/hierarchy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(1)));
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
    // POST — admin only
    // ---------------------------------------------------------------------------------------------

    @Test
    void create_saves_and_returns_category() throws Exception {
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

        JsonNode json = json(result);
        Long newId = json.get("id").asLong();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM category WHERE id = ?", newId);
        assertEquals("Books", row.get("name"));
        assertEquals("کتاب", row.get("local_name"));
        assertNull(row.get("parent_id"));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category", Integer.class));
    }

    @Test
    void create_with_parent_saves_category() throws Exception {
        var req = new CreateCategoryRequestDto();
        req.setName("Mobile");
        req.setLocalName("موبایل");
        req.setParentId(electronicsId);

        MvcResult result = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mobile"))
                .andExpect(jsonPath("$.localName").value("موبایل"))
                .andExpect(jsonPath("$.parentId").value(electronicsId))
                .andReturn();

        JsonNode json = json(result);
        Long newId = json.get("id").asLong();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM category WHERE id = ?", newId);
        assertEquals("Mobile", row.get("name"));
        assertEquals("موبایل", row.get("local_name"));
        assertEquals(electronicsId, row.get("parent_id"));
    }

    @Test
    void create_duplicate_name_returns_conflict_and_db_unchanged() throws Exception {
        var req = new CreateCategoryRequestDto();
        req.setName("Electronics");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NAME_ALREADY_EXISTS"));

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category", Integer.class));
        assertEquals("Electronics", jdbcTemplate.queryForObject(
                "SELECT name FROM category WHERE id = ?", String.class, electronicsId));
    }

    @Test
    void create_missing_name_returns_400_and_db_unchanged() throws Exception {
        var req = new CreateCategoryRequestDto();

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category", Integer.class));
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

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category", Integer.class));
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

    @Test
    void create_category_with_parent_not_found_returns_404_and_db_unchanged() throws Exception {
        var req = new CreateCategoryRequestDto();
        req.setName("Orphan");
        req.setParentId(9999L);

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NOT_FOUND"));

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category", Integer.class));
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
                .andExpect(jsonPath("$.id").value(electronicsId))
                .andExpect(jsonPath("$.name").value("Updated Electronics"))
                .andExpect(jsonPath("$.localName").value("الکترونیک جدید"));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM category WHERE id = ?", electronicsId);
        assertEquals("Updated Electronics", row.get("name"));
        assertEquals("الکترونیک جدید", row.get("local_name"));
        assertNull(row.get("parent_id"));
    }

    @Test
    void update_sets_parent_id() throws Exception {
        jdbcTemplate.update("INSERT INTO category (name) VALUES ('Clothing')");
        Long clothingId = jdbcTemplate.queryForObject(
                "SELECT id FROM category WHERE name = 'Clothing'", Long.class);

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
    void update_clears_parent_id_when_null() throws Exception {
        jdbcTemplate.update("UPDATE category SET parent_id = ? WHERE id = ?",
                electronicsId, electronicsId);

        var req = new UpdateCategoryRequestDto();
        req.setName("Electronics");
        req.setParentId(null);

        mockMvc.perform(put("/api/categories/{id}", electronicsId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId", nullValue()));

        assertNull(jdbcTemplate.queryForObject(
                "SELECT parent_id FROM category WHERE id = ?", Long.class, electronicsId));
    }

    @Test
    void update_not_found_returns_404() throws Exception {
        var req = new UpdateCategoryRequestDto();
        req.setName("Anything");

        mockMvc.perform(put("/api/categories/{id}", 9999)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void update_duplicate_name_returns_conflict_and_db_unchanged() throws Exception {
        jdbcTemplate.update("INSERT INTO category (name, local_name) VALUES (?, ?)",
                "Clothing", "لباس");

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
        assertEquals("الکترونیک", jdbcTemplate.queryForObject(
                "SELECT local_name FROM category WHERE id = ?", String.class, electronicsId));
    }

    @Test
    void update_same_name_is_allowed_and_db_unchanged() throws Exception {
        var req = new UpdateCategoryRequestDto();
        req.setName("Electronics");
        req.setLocalName("الکترونیک");

        mockMvc.perform(put("/api/categories/{id}", electronicsId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.localName").value("الکترونیک"));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM category WHERE id = ?", electronicsId);
        assertEquals("Electronics", row.get("name"));
        assertEquals("الکترونیک", row.get("local_name"));
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
    void delete_removes_category() throws Exception {
        Long toDelete = jdbcTemplate.queryForObject(
                "INSERT INTO category (name, local_name, parent_id) VALUES ('ToDelete', 'حذف', ?) RETURNING id",
                Long.class, electronicsId);

        mockMvc.perform(delete("/api/categories/{id}", toDelete)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category WHERE id = ?", Integer.class, toDelete);
        assertEquals(0, count);
        // Electronics should still exist
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category", Integer.class));
    }

    @Test
    void delete_not_found_returns_404_and_db_unchanged() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", 9999)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NOT_FOUND"));

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category", Integer.class));
    }

    @Test
    void delete_requires_admin_and_db_unchanged() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", electronicsId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category", Integer.class));
        assertEquals("Electronics", jdbcTemplate.queryForObject(
                "SELECT name FROM category WHERE id = ?", String.class, electronicsId));
    }

    @Test
    void delete_without_auth_returns_401() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", electronicsId))
                .andExpect(status().isUnauthorized());
    }
}
