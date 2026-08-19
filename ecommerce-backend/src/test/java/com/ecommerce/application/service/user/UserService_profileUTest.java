package com.ecommerce.application.service.user;

import com.ecommerce.application.api.dto.user.UpdateUserProfileRequestDto;
import com.ecommerce.application.api.dto.user.UserProfileResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.application.config.properties.LoginProperties;
import com.ecommerce.application.config.properties.SignupProperties;
import com.ecommerce.application.service.jwt.JwtService;
import com.ecommerce.application.service.ticket.LoginTicketService;
import com.ecommerce.application.service.ticket.SignupTicketService;
import com.ecommerce.persistence.cache.SignupCacheService;
import com.ecommerce.persistence.entity.AppUser;
import com.ecommerce.persistence.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserService_profileUTest {

    private static final Long USER_ID = 1L;

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private SignupTicketService signupTicketService;
    @Mock
    private LoginTicketService loginTicketService;
    @Mock
    private SignupCacheService signupCacheService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(appUserRepository, passwordEncoder, authenticationManager,
                jwtService, signupTicketService, loginTicketService, signupCacheService,
                new SignupProperties(), new LoginProperties());
    }

    @Test
    void getProfile_returns_name_and_mobile() {
        AppUser user = user("Sara", "Mohammadi", "09120000002");
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserProfileResponseDto response = userService.getProfile(USER_ID);

        assertEquals("Sara", response.getFirstName());
        assertEquals("Mohammadi", response.getLastName());
        assertEquals("09120000002", response.getMobile());
    }

    @Test
    void updateProfile_changes_mobile_and_username_when_they_match() {
        AppUser user = user("Sara", "Mohammadi", "09120000002");
        user.setUsername("09120000002");
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(appUserRepository.findByMobile("09121111111")).thenReturn(Optional.empty());
        when(appUserRepository.save(user)).thenReturn(user);

        UpdateUserProfileRequestDto request = request("Sara", "Karimi", "09121111111");
        UserProfileResponseDto response = userService.updateProfile(request, USER_ID);

        assertEquals("Karimi", user.getLastName());
        assertEquals("09121111111", user.getMobile());
        assertEquals("09121111111", user.getUsername());
        assertEquals("09121111111", response.getMobile());
    }

    @Test
    void updateProfile_rejects_mobile_taken_by_another_user() {
        AppUser user = user("Sara", "Mohammadi", "09120000002");
        AppUser other = user("Ali", "Karimi", "09121111111");
        other.setId(2L);
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(appUserRepository.findByMobile("09121111111")).thenReturn(Optional.of(other));

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> userService.updateProfile(request("Sara", "Mohammadi", "09121111111"), USER_ID));

        assertEquals(ECOMErrorType.USER_ALREADY_EXISTS, exception.getEcomErrorType());
    }

    private AppUser user(String firstName, String lastName, String mobile) {
        AppUser appUser = new AppUser();
        appUser.setId(USER_ID);
        appUser.setFirstName(firstName);
        appUser.setLastName(lastName);
        appUser.setMobile(mobile);
        appUser.setUsername(mobile);
        return appUser;
    }

    private UpdateUserProfileRequestDto request(String firstName, String lastName, String mobile) {
        UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setMobile(mobile);
        return request;
    }
}
