package com.ecommerce.application.service.order;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.checkout.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ReservationReleaseJob {

    private final ReservationReleaseService reservationReleaseService;

    @Scheduled(fixedDelayString = "${app.checkout.reservation-release-interval:60000}")
    public void run() {
        reservationReleaseService.releaseExpiredReservations();
    }
}
