package com.ecommerce.application.service.ticket;

import com.ecommerce.application.config.properties.LoginProperties;
import com.ecommerce.application.invoker.sms.SmsService;
import com.ecommerce.application.service.ticket.dto.TicketGenerateRequestDto;
import com.ecommerce.application.util.DateUtil;
import com.ecommerce.persistence.cache.BlockedMobileNumbersCacheService;
import com.ecommerce.persistence.cache.LoginTicketCacheService;
import com.ecommerce.persistence.repository.AppUserRepository;
import com.ecommerce.persistence.repository.MockOtpRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * @author AmirHossein ZamanZade
 * @since 5/29/26
 */
@Service
public class LoginTicketService extends AbstractTicketService {

    private final LoginProperties loginProperties;

    public LoginTicketService(DateUtil dateUtil, SmsService smsService, LoginProperties loginProperties,
            LoginTicketCacheService ticketCacheService,
            BlockedMobileNumbersCacheService blockedMobileNumbersCacheService,
            AppUserRepository appUserRepository, MockOtpRepository mockOtpRepository) {
        super(dateUtil, smsService, ticketCacheService, blockedMobileNumbersCacheService, appUserRepository,
                mockOtpRepository);
        this.loginProperties = loginProperties;
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
        return loginProperties.getTicket().getBlockDuration();
    }

    @Override
    protected Integer getMaxFailureCount() {
        return loginProperties.getTicket().getMaxFailureCount();
    }

    @Override
    protected int getTicketLength(TicketGenerateRequestDto ticketGenerateRequestDto) {
        return loginProperties.getTicket().getLength();
    }
}
