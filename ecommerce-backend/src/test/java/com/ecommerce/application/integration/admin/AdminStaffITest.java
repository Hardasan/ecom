package com.ecommerce.application.integration.admin;

import com.ecommerce.application.integration.AbstractIntegrationITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Admin management of warehouse-staff accounts: create, list, enable/disable, reset password, and security. */
class AdminStaffITest extends AbstractIntegrationITest {

    private static final String STAFF_MOBILE = "09120000005";

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void seedAdmin() throws Exception {
        String adminMobile = "09100000000";
        jdbcTemplate.update(
                "INSERT INTO app_user (first_name, last_name, username, mobile, password, role, is_enabled, is_registered) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "Admin", "User", adminMobile, adminMobile,
                passwordEncoder.encode("Admin123!"), "ROLE_ADMIN", true, true);
        adminToken = login(adminMobile, "Admin123!");
    }

    @Test
    void admin_creates_staff_who_can_login_as_warehouse() throws Exception {
        long staffId = createStaff("Ali", "Rezaei", STAFF_MOBILE, "secret1");

        mockMvc.perform(withAuth(get("/api/admin/staff"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(staffId))
                .andExpect(jsonPath("$[0].mobile").value(STAFF_MOBILE))
                .andExpect(jsonPath("$[0].enabled").value(true));

        MvcResult login = rawLogin(STAFF_MOBILE, "secret1").andExpect(status().isOk()).andReturn();
        assertEquals("ROLE_WAREHOUSE", json(login).get("role").asText());
        String staffToken = json(login).get("token").asText();

        // The new operator can reach the warehouse console but not admin-only management.
        mockMvc.perform(withAuth(get("/api/warehouse/orders"), staffToken))
                .andExpect(status().isOk());
        mockMvc.perform(withAuth(get("/api/admin/staff"), staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicate_mobile_is_rejected() throws Exception {
        createStaff("Ali", "Rezaei", STAFF_MOBILE, "secret1");

        postJson("/api/admin/staff",
                Map.of("firstName", "Other", "lastName", "Person", "mobile", STAFF_MOBILE, "password", "secret2"),
                adminToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USER_ALREADY_EXISTS"));
    }

    @Test
    void disabling_blocks_login_and_reset_password_restores_it() throws Exception {
        long staffId = createStaff("Ali", "Rezaei", STAFF_MOBILE, "secret1");

        mockMvc.perform(withAuth(patch("/api/admin/staff/{id}/status", staffId), adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        rawLogin(STAFF_MOBILE, "secret1").andExpect(status().isUnauthorized());

        mockMvc.perform(withAuth(patch("/api/admin/staff/{id}/status", staffId), adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("enabled", true))))
                .andExpect(status().isOk());

        postJson("/api/admin/staff/" + staffId + "/reset-password", Map.of("password", "newpass1"), adminToken)
                .andExpect(status().isOk());

        rawLogin(STAFF_MOBILE, "secret1").andExpect(status().isUnauthorized()); // old password no longer valid
        rawLogin(STAFF_MOBILE, "newpass1").andExpect(status().isOk());
    }

    @Test
    void status_change_on_unknown_or_non_warehouse_account_is_not_found() throws Exception {
        // Unknown id.
        mockMvc.perform(withAuth(patch("/api/admin/staff/{id}/status", 999999L), adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));

        // The admin's own (non-warehouse) account is out of scope for staff management.
        Long adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE role = 'ROLE_ADMIN'", Long.class);
        mockMvc.perform(withAuth(patch("/api/admin/staff/{id}/status", adminId), adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isNotFound());
    }

    @Test
    void validation_rejects_bad_mobile_and_short_password() throws Exception {
        postJson("/api/admin/staff",
                Map.of("firstName", "Ali", "lastName", "R", "mobile", "12345", "password", "secret1"), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        postJson("/api/admin/staff",
                Map.of("firstName", "Ali", "lastName", "R", "mobile", STAFF_MOBILE, "password", "12"), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void non_admin_cannot_manage_staff() throws Exception {
        String userToken = registerAndLogin(newMobile());

        mockMvc.perform(withAuth(get("/api/admin/staff"), userToken))
                .andExpect(status().isForbidden());
        postJson("/api/admin/staff",
                Map.of("firstName", "Ali", "lastName", "R", "mobile", STAFF_MOBILE, "password", "secret1"), userToken)
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/staff"))
                .andExpect(status().isUnauthorized());
    }

    private long createStaff(String first, String last, String mobile, String password) throws Exception {
        MvcResult result = postJson("/api/admin/staff",
                Map.of("firstName", first, "lastName", last, "mobile", mobile, "password", password), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn();
        return json(result).get("id").asLong();
    }

    private ResultActions rawLogin(String mobile, String password) throws Exception {
        return postJson("/api/user/login", Map.of("mobileNumber", mobile, "password", password));
    }
}
