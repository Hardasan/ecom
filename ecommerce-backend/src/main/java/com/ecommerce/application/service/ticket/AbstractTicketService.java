package com.ecommerce.application.service.ticket;


import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.application.invoker.sms.SmsService;
import com.ecommerce.application.service.ticket.dto.TicketGenerateRequestDto;
import com.ecommerce.application.util.DateUtil;
import com.ecommerce.persistence.cache.AbstractTicketCacheService;
import com.ecommerce.persistence.cache.BlockedMobileNumbersCacheService;
import com.ecommerce.persistence.cache.dto.TicketInfoCacheDto;
import com.ecommerce.persistence.entity.AppUser;
import com.ecommerce.persistence.entity.MockOtp;
import com.ecommerce.persistence.repository.AppUserRepository;
import com.ecommerce.persistence.repository.MockOtpRepository;
import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

/**
 * @author AmirHossein ZamanZade
 * @since 12/26/25
 */
@RequiredArgsConstructor
public abstract class AbstractTicketService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final DateUtil dateUtil;
    private final SmsService smsService;
    private final AbstractTicketCacheService ticketCacheService;
    private final BlockedMobileNumbersCacheService blockedMobileNumbersCacheService;
    private final AppUserRepository appUserRepository;
    private final MockOtpRepository mockOtpRepository;

    protected abstract Duration getBlockDuration();

    protected abstract Integer getMaxFailureCount();

    protected abstract int getTicketLength(TicketGenerateRequestDto ticketGenerateRequestDto);

    public void validateTicket(String cacheKey, String ticket, String mobileNumber) {
        if (mockOtpCode().filter(ticket::equals).isPresent()) {
            return;
        }
        TicketInfoCacheDto result = ticketCacheService.getTicketInfoDto(cacheKey);
        if (result == null || result.getTicket() == null || !result.getTicket().equals(ticket)) {
            handleFailureCount(result, cacheKey, mobileNumber);
            throw new EcommerceException(ECOMErrorType.INVALID_TICKET);
        }
    }

    public void deleteTicket(String cacheKey, Long userId) {
        ticketCacheService.deleteTicket(cacheKey, userId);
    }

    public void deleteLastSentDate(String cacheKey) {
        ticketCacheService.deleteLastSentTicketDate(cacheKey);
    }

    protected String prepareTicket(TicketGenerateRequestDto ticketGenerateRequestDto) {
        if (ticketCacheService.getTicketInfoDto(ticketGenerateRequestDto.getCacheKey()) != null) {
            throw new EcommerceException(ECOMErrorType.SEND_TICKET_TIME_LIMIT);
        }
        if (blockedMobileNumbersCacheService.isMobileNumberExistInBlockedMobileNumbers(ticketGenerateRequestDto
                .getMobileNumber())) {
            throw new EcommerceException(ECOMErrorType.TICKET_BLOCKED);
        }
        Date currentDate = new Date();
        long ticketTimeToLiveInMillis = (long) ticketGenerateRequestDto.getTicketTimeToLive() * 1000;
        Date lastSentTicketDate = ticketCacheService.getLastSentTicketDate(
                ticketGenerateRequestDto.getLastSentCacheKey());
        if (lastSentTicketDate != null && dateUtil.getDateDiffInMillis(lastSentTicketDate,
                currentDate) < ticketTimeToLiveInMillis) {
            throw new EcommerceException(ECOMErrorType.SEND_TICKET_TIME_LIMIT);
        }
        Date expireDate = new Date(currentDate.getTime() + ticketTimeToLiveInMillis);
        String ticket = generateTicket(getTicketLength(ticketGenerateRequestDto));
        Long userId = appUserRepository.findByMobile(ticketGenerateRequestDto.getMobileNumber())
                .map(AppUser::getId).orElse(null);
        ticketCacheService.addTicket(ticketGenerateRequestDto.getCacheKey(), userId,
                new TicketInfoCacheDto(ticket), expireDate);
        ticketCacheService.setLastSentTicketDate(ticketGenerateRequestDto.getLastSentCacheKey(), currentDate,
                expireDate);
        return ticket;
    }

    protected void sendTicketMessage(TicketGenerateRequestDto ticketGenerateRequestDto, String ticket) {
        if (mockOtpCode().isPresent()) {
            return;
        }
        try {
            smsService.sendOTP(ticketGenerateRequestDto.getSmsTemplateId(), ticketGenerateRequestDto.getMobileNumber(),
                    ticket, ticketGenerateRequestDto.getTicketTimeToLive() / 60);
        } catch (Throwable exception) {
            ticketCacheService.deleteTicket(ticketGenerateRequestDto.getCacheKey(), null);
            ticketCacheService.deleteLastSentTicketDate(ticketGenerateRequestDto.getLastSentCacheKey());
            throw exception;
        }
    }

    private String generateTicket(int ticketLength) {
        return mockOtpCode().orElseGet(() -> {
            int bound = (int) Math.pow(10, ticketLength);
            return String.format("%0" + ticketLength + "d", SECURE_RANDOM.nextInt(bound));
        });
    }

    private Optional<String> mockOtpCode() {
        return mockOtpRepository.findFirstByOrderByIdAsc()
                .map(MockOtp::getCode)
                .filter(code -> code != null && !code.isBlank());
    }

    private void handleFailureCount(TicketInfoCacheDto result, String ticketCacheKey, String mobileNumber) {
        if (result != null) {
            int failureCount = result.incrementAndGetFailureCount();
            if (failureCount >= getMaxFailureCount()) {
                deleteTicket(ticketCacheKey, null);
                blockedMobileNumbersCacheService.addMobileNumber(mobileNumber, getBlockDuration().toSeconds());
                throw new EcommerceException(ECOMErrorType.TICKET_BLOCKED);
            }
            ticketCacheService.updateTicketInfoDto(ticketCacheKey, result);
        }
    }
}
