package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.admin.CreateStaffRequestDto;
import com.ecommerce.application.api.dto.admin.ResetStaffPasswordRequestDto;
import com.ecommerce.application.api.dto.admin.StaffResponseDto;
import com.ecommerce.application.api.dto.admin.UpdateStaffStatusRequestDto;
import com.ecommerce.application.service.admin.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin-only management of warehouse-staff accounts: create an operator, list them, enable/disable a
 * login, or reset a password. Admin-gated at the class level.
 */
@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffController {

    private final StaffService staffService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<StaffResponseDto> list() {
        return staffService.listStaff();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public StaffResponseDto create(@RequestBody CreateStaffRequestDto requestDto) {
        return staffService.create(requestDto);
    }

    @PatchMapping(value = "/{staffId}/status",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public StaffResponseDto setStatus(@PathVariable Long staffId,
                                      @RequestBody UpdateStaffStatusRequestDto requestDto) {
        return staffService.setEnabled(staffId, requestDto.getEnabled());
    }

    @PostMapping(value = "/{staffId}/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void resetPassword(@PathVariable Long staffId,
                              @RequestBody ResetStaffPasswordRequestDto requestDto) {
        staffService.resetPassword(staffId, requestDto.getPassword());
    }
}
