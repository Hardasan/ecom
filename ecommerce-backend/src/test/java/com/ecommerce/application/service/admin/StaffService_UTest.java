package com.ecommerce.application.service.admin;

import com.ecommerce.application.api.dto.admin.CreateStaffRequestDto;
import com.ecommerce.application.api.dto.admin.StaffResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.AppUser;
import com.ecommerce.persistence.entity.enumeration.UserRole;
import com.ecommerce.persistence.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffService_UTest {

    private static final Long STAFF_ID = 5L;

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private StaffService staffService;

    @BeforeEach
    void setUp() {
        staffService = new StaffService(appUserRepository, passwordEncoder);
        lenient().when(passwordEncoder.encode(any())).thenAnswer(inv -> "enc(" + inv.getArgument(0) + ")");
        lenient().when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_persists_enabled_registered_warehouse_account_with_encoded_password() {
        when(appUserRepository.existsByMobile("09120000001")).thenReturn(false);

        StaffResponseDto dto = staffService.create(request("Ali", "Rezaei", "09120000001", "secret1"));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertEquals(UserRole.ROLE_WAREHOUSE, saved.getRole());
        assertTrue(saved.getIsEnabled());
        assertTrue(saved.getIsRegistered());
        assertEquals("09120000001", saved.getMobile());
        assertEquals("09120000001", saved.getUsername());
        assertEquals("enc(secret1)", saved.getPassword());
        assertEquals("Ali", dto.getFirstName());
        assertTrue(dto.isEnabled());
    }

    @Test
    void create_rejects_duplicate_mobile() {
        when(appUserRepository.existsByMobile("09120000001")).thenReturn(true);

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> staffService.create(request("Ali", "Rezaei", "09120000001", "secret1")));
        assertEquals(ECOMErrorType.USER_ALREADY_EXISTS, ex.getEcomErrorType());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void listStaff_maps_warehouse_users() {
        when(appUserRepository.findByRoleOrderByIdDesc(UserRole.ROLE_WAREHOUSE))
                .thenReturn(List.of(staff(true), staff(false)));

        List<StaffResponseDto> list = staffService.listStaff();

        assertEquals(2, list.size());
        assertEquals(STAFF_ID, list.getFirst().getId());
    }

    @Test
    void setEnabled_toggles_flag() {
        AppUser staff = staff(true);
        when(appUserRepository.findById(STAFF_ID)).thenReturn(Optional.of(staff));

        StaffResponseDto dto = staffService.setEnabled(STAFF_ID, false);

        assertFalse(staff.getIsEnabled());
        assertFalse(dto.isEnabled());
    }

    @Test
    void setEnabled_rejects_non_warehouse_account() {
        AppUser admin = staff(true);
        admin.setRole(UserRole.ROLE_ADMIN);
        when(appUserRepository.findById(STAFF_ID)).thenReturn(Optional.of(admin));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> staffService.setEnabled(STAFF_ID, false));
        assertEquals(ECOMErrorType.USER_NOT_FOUND, ex.getEcomErrorType());
    }

    @Test
    void resetPassword_encodes_new_password() {
        AppUser staff = staff(true);
        when(appUserRepository.findById(STAFF_ID)).thenReturn(Optional.of(staff));

        staffService.resetPassword(STAFF_ID, "brandnew");

        assertEquals("enc(brandnew)", staff.getPassword());
    }

    @Test
    void resetPassword_rejects_unknown_id() {
        when(appUserRepository.findById(STAFF_ID)).thenReturn(Optional.empty());

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> staffService.resetPassword(STAFF_ID, "brandnew"));
        assertEquals(ECOMErrorType.USER_NOT_FOUND, ex.getEcomErrorType());
    }

    private CreateStaffRequestDto request(String first, String last, String mobile, String password) {
        CreateStaffRequestDto dto = new CreateStaffRequestDto();
        dto.setFirstName(first);
        dto.setLastName(last);
        dto.setMobile(mobile);
        dto.setPassword(password);
        return dto;
    }

    private AppUser staff(boolean enabled) {
        AppUser user = new AppUser();
        user.setId(STAFF_ID);
        user.setFirstName("Ali");
        user.setLastName("Rezaei");
        user.setMobile("09120000001");
        user.setUsername("09120000001");
        user.setPassword("enc(old)");
        user.setRole(UserRole.ROLE_WAREHOUSE);
        user.setIsEnabled(enabled);
        user.setIsRegistered(true);
        return user;
    }
}
