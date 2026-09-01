package com.ecommerce.application.service.user;

import com.ecommerce.application.api.dto.user.LoginRequestDto;
import com.ecommerce.application.api.dto.user.LoginResponseDto;
import com.ecommerce.application.api.dto.user.enumeration.Role;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.application.config.properties.LoginProperties;
import com.ecommerce.application.config.properties.SignupProperties;
import com.ecommerce.application.config.security.UserDetailsDto;
import com.ecommerce.application.service.jwt.JwtService;
import com.ecommerce.application.service.ticket.LoginTicketService;
import com.ecommerce.application.service.ticket.SignupTicketService;
import com.ecommerce.persistence.cache.SignupCacheService;
import com.ecommerce.persistence.entity.AppUser;
import com.ecommerce.persistence.entity.enumeration.UserRole;
import com.ecommerce.persistence.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserService_loginUTest {

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
    void valid_credentials_return_jwt_and_role() {
        AppUser appUser = new AppUser();
        appUser.setId(1L);
        appUser.setUsername("09121111118");
        appUser.setPassword("encoded-password");
        appUser.setRole(UserRole.ROLE_APP_USER);
        appUser.setIsEnabled(true);
        appUser.setIsRegistered(true);
        UserDetailsDto userDetailsDto = new UserDetailsDto(appUser);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetailsDto);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(userDetailsDto)).thenReturn("jwt-token");

        LoginResponseDto responseDto = userService.login(requestDto("09121111118", "my-password"));

        assertEquals("jwt-token", responseDto.getToken());
        assertEquals(Role.ROLE_APP_USER, responseDto.getRole());
    }

    @Test
    void authentication_uses_mobile_and_password_from_request() {
        AppUser appUser = new AppUser();
        appUser.setId(1L);
        appUser.setUsername("09121111118");
        appUser.setPassword("encoded-password");
        appUser.setRole(UserRole.ROLE_APP_USER);
        appUser.setIsEnabled(true);
        appUser.setIsRegistered(true);
        UserDetailsDto userDetailsDto = new UserDetailsDto(appUser);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetailsDto);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        userService.login(requestDto("09121111118", "my-password"));

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("09121111118", captor.getValue().getPrincipal());
        assertEquals("my-password", captor.getValue().getCredentials());
    }

    @Test
    void bad_credentials_map_to_invalid_credentials_error() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> userService.login(requestDto("09121111118", "wrong")));
        assertEquals(ECOMErrorType.INVALID_CREDENTIALS, exception.getEcomErrorType());
    }

    private LoginRequestDto requestDto(String mobile, String password) {
        LoginRequestDto requestDto = new LoginRequestDto();
        requestDto.setMobileNumber(mobile);
        requestDto.setPassword(password);
        return requestDto;
    }
}
