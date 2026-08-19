package com.ecommerce.application.service.ticket;

import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.application.config.properties.LoginProperties;
import com.ecommerce.application.config.properties.TicketProperties;
import com.ecommerce.application.invoker.sms.SmsService;
import com.ecommerce.application.util.DateUtil;
import com.ecommerce.persistence.cache.BlockedMobileNumbersCacheService;
import com.ecommerce.persistence.cache.LoginTicketCacheService;
import com.ecommerce.persistence.cache.dto.TicketInfoCacheDto;
import com.ecommerce.persistence.entity.MockOtp;
import com.ecommerce.persistence.repository.AppUserRepository;
import com.ecommerce.persistence.repository.MockOtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginTicketService_validateTicketUTest {

    @Mock
    private DateUtil dateUtil;
    @Mock
    private SmsService smsService;
    @Mock
    private LoginTicketCacheService ticketCacheService;
    @Mock
    private BlockedMobileNumbersCacheService blockedMobileNumbersCacheService;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private MockOtpRepository mockOtpRepository;

    private LoginTicketService loginTicketService;

    @BeforeEach
    void setUp() {
        LoginProperties loginProperties = new LoginProperties();
        TicketProperties ticketProperties = new TicketProperties();
        ticketProperties.setMaxFailureCount(2);
        ticketProperties.setBlockDuration(Duration.ofMinutes(10));
        loginProperties.setTicket(ticketProperties);
        loginTicketService = new LoginTicketService(dateUtil, smsService, loginProperties, ticketCacheService,
                blockedMobileNumbersCacheService, appUserRepository, mockOtpRepository);
    }

    @Test
    void valid_ticket_deletes_ticket_from_cache_after_validation() {
        when(ticketCacheService.getTicketInfoDto("cache-key")).thenReturn(new TicketInfoCacheDto("123456"));

        loginTicketService.validateTicket("cache-key", "123456", "09121111118");

        verify(ticketCacheService).deleteTicket("cache-key", null);
    }

    @Test
    void first_invalid_ticket_increments_failure_count_without_blocking() {
        TicketInfoCacheDto ticketInfoCacheDto = new TicketInfoCacheDto("123456");
        when(ticketCacheService.getTicketInfoDto("cache-key")).thenReturn(ticketInfoCacheDto);

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> loginTicketService.validateTicket("cache-key", "000000", "09121111118"));

        assertEquals(ECOMErrorType.INVALID_TICKET, exception.getEcomErrorType());
        verify(ticketCacheService).updateTicketInfoDto("cache-key", ticketInfoCacheDto);
        verify(blockedMobileNumbersCacheService, never())
                .addMobileNumber(any(), anyLong());
    }

    @Test
    void reaching_max_failure_count_blocks_mobile_number() {
        TicketInfoCacheDto ticketInfoCacheDto = new TicketInfoCacheDto("123456");
        ticketInfoCacheDto.incrementAndGetFailureCount();
        when(ticketCacheService.getTicketInfoDto("cache-key")).thenReturn(ticketInfoCacheDto);

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> loginTicketService.validateTicket("cache-key", "000000", "09121111118"));

        assertEquals(ECOMErrorType.TICKET_BLOCKED, exception.getEcomErrorType());
        verify(ticketCacheService).deleteTicket("cache-key", null);
        verify(blockedMobileNumbersCacheService).addMobileNumber("09121111118", 600L);
    }

    @Test
    void null_ticket_in_cache_throws_invalid_ticket_exception() {
        when(ticketCacheService.getTicketInfoDto("cache-key")).thenReturn(null);

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> loginTicketService.validateTicket("cache-key", "123456", "09121111118"));

        assertEquals(ECOMErrorType.INVALID_TICKET, exception.getEcomErrorType());
    }

    @Test
    void mock_otp_is_accepted_even_when_cache_is_empty() {
        MockOtp mockOtp = new MockOtp();
        mockOtp.setCode("123456");
        when(mockOtpRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(mockOtp));

        loginTicketService.validateTicket("cache-key", "123456", "09121111118");

        verify(ticketCacheService).deleteTicket("cache-key", null);
        verify(ticketCacheService, never()).getTicketInfoDto(any());
    }
}
