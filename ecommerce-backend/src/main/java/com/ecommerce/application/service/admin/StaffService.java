package com.ecommerce.application.service.admin;

import com.ecommerce.application.api.dto.admin.CreateStaffRequestDto;
import com.ecommerce.application.api.dto.admin.StaffResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.AppUser;
import com.ecommerce.persistence.entity.enumeration.UserRole;
import com.ecommerce.persistence.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin management of warehouse-staff accounts (ROLE_WAREHOUSE). Every mutation is scoped to
 * warehouse users only, so this endpoint can never touch an admin or a shopper account: a lookup by
 * id that is not a warehouse account is reported as {@code USER_NOT_FOUND}.
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<StaffResponseDto> listStaff() {
        return appUserRepository.findByRoleOrderByIdDesc(UserRole.ROLE_WAREHOUSE).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public StaffResponseDto create(CreateStaffRequestDto requestDto) {
        if (appUserRepository.existsByMobile(requestDto.getMobile())) {
            throw new EcommerceException(ECOMErrorType.USER_ALREADY_EXISTS);
        }
        AppUser staff = new AppUser();
        staff.setFirstName(requestDto.getFirstName());
        staff.setLastName(requestDto.getLastName());
        staff.setUsername(requestDto.getMobile());
        staff.setMobile(requestDto.getMobile());
        staff.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        staff.setRole(UserRole.ROLE_WAREHOUSE);
        staff.setIsEnabled(true);
        staff.setIsRegistered(true);
        return toDto(appUserRepository.save(staff));
    }

    @Transactional
    public StaffResponseDto setEnabled(Long staffId, boolean enabled) {
        AppUser staff = requireStaff(staffId);
        staff.setIsEnabled(enabled);
        return toDto(appUserRepository.save(staff));
    }

    @Transactional
    public void resetPassword(Long staffId, String newPassword) {
        AppUser staff = requireStaff(staffId);
        staff.setPassword(passwordEncoder.encode(newPassword));
        appUserRepository.save(staff);
    }

    private AppUser requireStaff(Long staffId) {
        return appUserRepository.findById(staffId)
                .filter(user -> user.getRole() == UserRole.ROLE_WAREHOUSE)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.USER_NOT_FOUND));
    }

    private StaffResponseDto toDto(AppUser staff) {
        return new StaffResponseDto(staff.getId(), staff.getFirstName(), staff.getLastName(),
                staff.getMobile(), Boolean.TRUE.equals(staff.getIsEnabled()), staff.getCreatedAt());
    }
}
