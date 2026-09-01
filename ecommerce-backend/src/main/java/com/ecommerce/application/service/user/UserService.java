package com.ecommerce.application.service.user;

import com.ecommerce.application.api.dto.user.*;
import com.ecommerce.application.api.dto.user.enumeration.Role;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.application.config.properties.LoginProperties;
import com.ecommerce.application.config.properties.SignupProperties;
import com.ecommerce.application.config.properties.TicketProperties;
import com.ecommerce.application.config.security.UserDetailsDto;
import com.ecommerce.application.service.jwt.JwtService;
import com.ecommerce.application.service.ticket.LoginTicketService;
import com.ecommerce.application.service.ticket.SignupTicketService;
import com.ecommerce.application.service.ticket.dto.TicketGenerateRequestDto;
import com.ecommerce.persistence.cache.SignupCacheService;
import com.ecommerce.persistence.cache.dto.SignupData;
import com.ecommerce.persistence.entity.AppUser;
import com.ecommerce.persistence.entity.enumeration.UserRole;
import com.ecommerce.persistence.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * @author AmirHossein ZamanZade
 * @since 12/26/25
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SignupTicketService signupTicketService;
    private final LoginTicketService loginTicketService;
    private final SignupCacheService signupCacheService;
    private final SignupProperties signupProperties;
    private final LoginProperties loginProperties;

    public SendSignupTicketResponseDto sendSignupTicket(SendSignupTicketRequestDto requestDto) {
        signupTicketService.sendTicket(buildTicketRequest(requestDto.getMobileNumber(),
                signupProperties.getTicket()));
        return new SendSignupTicketResponseDto(signupProperties.getTicket().getTimeToLive().toSeconds());
    }

    public SignupTicketValidationResponseDto validateSignupTicket(SignupTicketValidationRequestDto requestDto) {
        String mobileNumber = requestDto.getMobileNumber();
        signupTicketService.validateTicket(mobileNumber, requestDto.getTicket(), mobileNumber);
        String signupToken = UUID.randomUUID().toString();
        signupCacheService.addSignupData(signupToken, new SignupData(mobileNumber),
                signupProperties.getTokenTtl().toSeconds());
        return new SignupTicketValidationResponseDto(signupToken);
    }

    @Transactional
    public void signup(SignupRequestDto requestDto) {
        SignupData signupData = signupCacheService.getSignupData(requestDto.getSignupToken());
        if (signupData == null) {
            throw new EcommerceException(ECOMErrorType.INVALID_SIGNUP_TOKEN);
        }
        Optional<AppUser> existingUser = appUserRepository.findByMobile(signupData.getMobile());
        if (existingUser.isPresent() && Boolean.TRUE.equals(existingUser.get().getIsRegistered())) {
            throw new EcommerceException(ECOMErrorType.USER_ALREADY_EXISTS);
        }
        AppUser appUser = existingUser.orElse(new AppUser());
        appUser.setFirstName(requestDto.getFirstName());
        appUser.setLastName(requestDto.getLastName());
        appUser.setUsername(signupData.getMobile());
        appUser.setMobile(signupData.getMobile());
        appUser.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        appUser.setRole(UserRole.ROLE_APP_USER);
        appUser.setIsEnabled(true);
        appUser.setIsRegistered(true);
        appUserRepository.save(appUser);
        signupCacheService.deleteSignupData(requestDto.getSignupToken());
    }

    public CheckUserRegistrationResponseDto checkUserRegistration(CheckUserRegistrationRequestDto requestDto) {
        boolean isRegistered = appUserRepository.findByMobile(requestDto.getMobileNumber())
                .map(u -> Boolean.TRUE.equals(u.getIsRegistered()))
                .orElse(false);
        return new CheckUserRegistrationResponseDto(isRegistered);
    }

    public LoginResponseDto login(LoginRequestDto requestDto) {
        Authentication authenticate;
        try {
            authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestDto.getMobileNumber(), requestDto.getPassword()));
        } catch (AuthenticationException e) {
            // Wrong mobile/password (or disabled account): surface a clear, localized message instead
            // of the generic "unexpected error". A uniform message avoids leaking which field was wrong.
            throw new EcommerceException(ECOMErrorType.INVALID_CREDENTIALS);
        }
        UserDetailsDto userDetails = (UserDetailsDto) authenticate.getPrincipal();
        return new LoginResponseDto(jwtService.generateToken(userDetails),
                Role.valueOf(userDetails.getAuthorities().getFirst().getAuthority()));
    }

    public SendSignupTicketResponseDto sendLoginTicket(SendLoginTicketRequestDto requestDto) {
        appUserRepository.findByMobile(requestDto.getMobileNumber())
                .filter(u -> Boolean.TRUE.equals(u.getIsRegistered()))
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.USER_NOT_FOUND));
        loginTicketService.sendTicket(buildTicketRequest(requestDto.getMobileNumber(),
                loginProperties.getTicket()));
        return new SendSignupTicketResponseDto(loginProperties.getTicket().getTimeToLive().toSeconds());
    }

    public LoginResponseDto validateLoginTicket(ValidateLoginTicketRequestDto requestDto) {
        String mobileNumber = requestDto.getMobileNumber();
        loginTicketService.validateTicket(mobileNumber, requestDto.getTicket(), mobileNumber);
        AppUser appUser = appUserRepository.findByMobile(mobileNumber)
                .filter(u -> Boolean.TRUE.equals(u.getIsRegistered()))
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.USER_NOT_FOUND));
        UserDetailsDto userDetails = new UserDetailsDto(appUser);
        return new LoginResponseDto(jwtService.generateToken(userDetails),
                Role.valueOf(userDetails.getAuthorities().getFirst().getAuthority()));
    }

    @Transactional
    public void changePassword(ChangePasswordRequestDto requestDto, Long userId) {
        if (!requestDto.getNewPassword().equals(requestDto.getConfirmPassword())) {
            throw new EcommerceException(ECOMErrorType.INVALID_PASSWORD);
        }
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.USER_NOT_FOUND));
        appUser.setPassword(passwordEncoder.encode(requestDto.getNewPassword()));
        appUserRepository.save(appUser);
    }

    @Transactional
    public IbanResponseDto updateIban(UpdateIbanRequestDto requestDto, Long userId) {
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.USER_NOT_FOUND));
        appUser.setIban(requestDto.getIban());
        appUserRepository.save(appUser);
        return new IbanResponseDto(appUser.getIban());
    }

    @Transactional(readOnly = true)
    public IbanResponseDto getIban(Long userId) {
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.USER_NOT_FOUND));
        return new IbanResponseDto(appUser.getIban());
    }

    @Transactional(readOnly = true)
    public UserProfileResponseDto getProfile(Long userId) {
        AppUser appUser = requireUser(userId);
        return toProfile(appUser);
    }

    @Transactional
    public UserProfileResponseDto updateProfile(UpdateUserProfileRequestDto requestDto, Long userId) {
        AppUser appUser = requireUser(userId);
        String newMobile = requestDto.getMobile();
        if (!newMobile.equals(appUser.getMobile())) {
            appUserRepository.findByMobile(newMobile)
                    .filter(other -> !other.getId().equals(userId))
                    .ifPresent(other -> {
                        throw new EcommerceException(ECOMErrorType.USER_ALREADY_EXISTS);
                    });
            if (appUser.getMobile() != null && appUser.getMobile().equals(appUser.getUsername())) {
                appUser.setUsername(newMobile);
            }
            appUser.setMobile(newMobile);
        }
        appUser.setFirstName(requestDto.getFirstName());
        appUser.setLastName(requestDto.getLastName());
        return toProfile(appUserRepository.save(appUser));
    }

    private AppUser requireUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.USER_NOT_FOUND));
    }

    private UserProfileResponseDto toProfile(AppUser appUser) {
        return new UserProfileResponseDto(appUser.getFirstName(), appUser.getLastName(), appUser.getMobile());
    }

    private TicketGenerateRequestDto buildTicketRequest(String mobileNumber, TicketProperties ticketProperties) {
        TicketGenerateRequestDto dto = new TicketGenerateRequestDto();
        dto.setMobileNumber(mobileNumber);
        dto.setCacheKey(mobileNumber);
        dto.setLastSentCacheKey(mobileNumber);
        dto.setSmsTemplateId(ticketProperties.getTemplateId());
        dto.setTicketTimeToLive((int) ticketProperties.getTimeToLive().toSeconds());
        return dto;
    }
}
