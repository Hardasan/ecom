package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.user.*;
import com.ecommerce.application.config.security.UserDetailsDto;
import com.ecommerce.application.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author AmirHossein ZamanZade
 * @since 12/26/25
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping(value = "/signup-ticket", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SendSignupTicketResponseDto sendSignupTicket(@RequestBody SendSignupTicketRequestDto requestDto) {
        return userService.sendSignupTicket(requestDto);
    }

    @PostMapping(value = "/signup-ticket/validation", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SignupTicketValidationResponseDto validateSignupTicket(
            @RequestBody SignupTicketValidationRequestDto requestDto) {
        return userService.validateSignupTicket(requestDto);
    }

    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void signup(@RequestBody SignupRequestDto requestDto) {
        userService.signup(requestDto);
    }

    @PostMapping(value = "/check-registration", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CheckUserRegistrationResponseDto checkUserRegistration(
            @RequestBody CheckUserRegistrationRequestDto requestDto) {
        return userService.checkUserRegistration(requestDto);
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public LoginResponseDto login(@RequestBody LoginRequestDto requestDto) {
        return userService.login(requestDto);
    }

    @PostMapping(value = "/login-ticket", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SendSignupTicketResponseDto sendLoginTicket(@RequestBody SendLoginTicketRequestDto requestDto) {
        return userService.sendLoginTicket(requestDto);
    }

    @PostMapping(value = "/login-ticket/validation", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public LoginResponseDto validateLoginTicket(@RequestBody ValidateLoginTicketRequestDto requestDto) {
        return userService.validateLoginTicket(requestDto);
    }

    @PostMapping(value = "/change-password", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void changePassword(@RequestBody ChangePasswordRequestDto requestDto, Authentication authentication) {
        UserDetailsDto userDetails = (UserDetailsDto) authentication.getPrincipal();
        userService.changePassword(requestDto, userDetails.getId());
    }

    @GetMapping(value = "/iban", produces = MediaType.APPLICATION_JSON_VALUE)
    public IbanResponseDto getIban(Authentication authentication) {
        return userService.getIban(userId(authentication));
    }

    @PutMapping(value = "/iban", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public IbanResponseDto updateIban(@Valid @RequestBody UpdateIbanRequestDto requestDto,
                                      Authentication authentication) {
        return userService.updateIban(requestDto, userId(authentication));
    }

    private Long userId(Authentication authentication) {
        return ((UserDetailsDto) authentication.getPrincipal()).getId();
    }
}
