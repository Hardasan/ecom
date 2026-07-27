package com.ecommerce.application.service.user;

import com.ecommerce.application.api.dto.user.IbanResponseDto;
import com.ecommerce.application.api.dto.user.UpdateIbanRequestDto;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserService_ibanUTest {

    private static final Long USER_ID = 1L;
    private static final String IBAN = "IR062960000000100324200001";

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
    void updateIban_persists_value() {
        AppUser user = new AppUser();
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UpdateIbanRequestDto request = new UpdateIbanRequestDto();
        request.setIban(IBAN);

        IbanResponseDto response = userService.updateIban(request, USER_ID);

        assertEquals(IBAN, user.getIban());
        assertEquals(IBAN, response.getIban());
        verify(appUserRepository).save(user);
    }

    @Test
    void getIban_returns_stored_value() {
        AppUser user = new AppUser();
        user.setIban(IBAN);
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertEquals(IBAN, userService.getIban(USER_ID).getIban());
    }

    @Test
    void getIban_returns_null_when_unset() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(new AppUser()));

        assertNull(userService.getIban(USER_ID).getIban());
    }

    @Test
    void updateIban_user_not_found() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        UpdateIbanRequestDto request = new UpdateIbanRequestDto();
        request.setIban(IBAN);

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> userService.updateIban(request, USER_ID));
        assertEquals(ECOMErrorType.USER_NOT_FOUND, ex.getEcomErrorType());
    }
}
