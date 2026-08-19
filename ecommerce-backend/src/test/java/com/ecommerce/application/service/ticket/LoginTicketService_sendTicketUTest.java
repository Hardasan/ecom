package com.ecommerce.application.service.ticket;

import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.application.config.properties.LoginProperties;
import com.ecommerce.application.config.properties.TicketProperties;
import com.ecommerce.application.invoker.sms.SmsService;
import com.ecommerce.application.service.ticket.dto.TicketGenerateRequestDto;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginTicketService_sendTicketUTest {

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
        ticketProperties.setLength(6);
        loginProperties.setTicket(ticketProperties);
        loginTicketService = new LoginTicketService(dateUtil, smsService, loginProperties, ticketCacheService,
                blockedMobileNumbersCacheService, appUserRepository, mockOtpRepository);
    }

    @Test
    void successful_send_ticket_caches_six_digit_ticket_and_sends_sms() throws Exception {
        TicketGenerateRequestDto requestDto = requestDto();

        loginTicketService.sendTicket(requestDto);

        ArgumentCaptor<TicketInfoCacheDto> captor = ArgumentCaptor.forClass(TicketInfoCacheDto.class);
        verify(ticketCacheService).addTicket(org.mockito.ArgumentMatchers.eq("ticket-cache"),
                org.mockito.ArgumentMatchers.isNull(),
                captor.capture(), org.mockito.ArgumentMatchers.any());
        assertTrue(captor.getValue().getTicket().matches("\\d{6}"));
        verify(ticketCacheService).setLastSentTicketDate(org.mockito.ArgumentMatchers.eq("last-sent-cache"),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(smsService).sendOTP(org.mockito.ArgumentMatchers.eq(20), org.mockito.ArgumentMatchers.eq("09121111118"),
                org.mockito.ArgumentMatchers.matches("\\d{6}"), org.mockito.ArgumentMatchers.eq(2));
    }

    @Test
    void existing_ticket_throws_time_limit_exception_without_sending_sms() {
        when(ticketCacheService.getTicketInfoDto("ticket-cache")).thenReturn(new TicketInfoCacheDto("123456"));

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> loginTicketService.sendTicket(requestDto()));

        assertEquals(ECOMErrorType.SEND_TICKET_TIME_LIMIT, exception.getEcomErrorType());

        verify(smsService, never()).sendOTP(any(), any(), any(), any());
    }

    @Test
    void blocked_mobile_throws_block_exception_without_sending_sms() {
        when(blockedMobileNumbersCacheService.isMobileNumberExistInBlockedMobileNumbers("09121111118"))
                .thenReturn(true);

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> loginTicketService.sendTicket(requestDto()));

        assertEquals(ECOMErrorType.TICKET_BLOCKED, exception.getEcomErrorType());

        verify(smsService, never()).sendOTP(any(), any(), any(), any());
    }

    @Test
    void sms_failure_removes_created_cache_entries() {
        doThrow(new RuntimeException("sms failed")).when(smsService)
                .sendOTP(org.mockito.ArgumentMatchers.eq(20), org.mockito.ArgumentMatchers.eq("09121111118"),
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(2));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> loginTicketService.sendTicket(requestDto()));

        assertEquals("sms failed", exception.getMessage());
        verify(ticketCacheService).deleteTicket("ticket-cache", null);
        verify(ticketCacheService).deleteLastSentTicketDate("last-sent-cache");
    }

    @Test
    void mock_otp_uses_db_code_and_skips_sms() {
        MockOtp mockOtp = new MockOtp();
        mockOtp.setCode("123456");
        when(mockOtpRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(mockOtp));

        loginTicketService.sendTicket(requestDto());

        ArgumentCaptor<TicketInfoCacheDto> captor = ArgumentCaptor.forClass(TicketInfoCacheDto.class);
        verify(ticketCacheService).addTicket(org.mockito.ArgumentMatchers.eq("ticket-cache"),
                org.mockito.ArgumentMatchers.isNull(),
                captor.capture(), org.mockito.ArgumentMatchers.any());
        assertEquals("123456", captor.getValue().getTicket());
        verify(smsService, never()).sendOTP(any(), any(), any(), any());
    }

    private TicketGenerateRequestDto requestDto() {
        TicketGenerateRequestDto requestDto = new TicketGenerateRequestDto();
        requestDto.setCacheKey("ticket-cache");
        requestDto.setLastSentCacheKey("last-sent-cache");
        requestDto.setMobileNumber("09121111118");
        requestDto.setSmsTemplateId(20);
        requestDto.setTicketTimeToLive(120);
        return requestDto;
    }
}
