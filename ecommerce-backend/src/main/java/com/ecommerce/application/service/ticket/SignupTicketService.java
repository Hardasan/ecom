package com.ecommerce.application.service.ticket;


import com.ecommerce.application.config.properties.SignupProperties;
import com.ecommerce.application.invoker.sms.SmsService;
import com.ecommerce.application.service.ticket.dto.TicketGenerateRequestDto;
import com.ecommerce.application.util.DateUtil;
import com.ecommerce.persistence.cache.BlockedMobileNumbersCacheService;
import com.ecommerce.persistence.cache.SignupTicketCacheService;
import com.ecommerce.persistence.repository.AppUserRepository;
import com.ecommerce.persistence.repository.MockOtpRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * @author AmirHossein ZamanZade
 * @since 12/26/25
 */
@Service
public class SignupTicketService extends AbstractTicketService {

    private final SignupProperties signupProperties;

    public SignupTicketService(DateUtil dateUtil, SmsService smsService, SignupProperties signupProperties,
            SignupTicketCacheService ticketCacheService,
            BlockedMobileNumbersCacheService blockedMobileNumbersCacheService,
            AppUserRepository appUserRepository, MockOtpRepository mockOtpRepository) {
        super(dateUtil, smsService, ticketCacheService, blockedMobileNumbersCacheService, appUserRepository,
                mockOtpRepository);
        this.signupProperties = signupProperties;
    }

    public void sendTicket(TicketGenerateRequestDto ticketGenerateRequestDto) {
        sendTicketMessage(ticketGenerateRequestDto, prepareTicket(ticketGenerateRequestDto));
    }

    @Override
    public void validateTicket(String cacheKey, String ticket, String mobileNumber) {
        super.validateTicket(cacheKey, ticket, mobileNumber);
        deleteTicket(cacheKey, null);
    }

    @Override
    protected Duration getBlockDuration() {
        return signupProperties.getTicket().getBlockDuration();
    }

    @Override
    protected Integer getMaxFailureCount() {
        return signupProperties.getTicket().getMaxFailureCount();
    }

    @Override
    protected int getTicketLength(TicketGenerateRequestDto ticketGenerateRequestDto) {
        return signupProperties.getTicket().getLength();
    }
}
