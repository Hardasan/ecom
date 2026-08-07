package com.ecommerce.application.service.order;

import com.ecommerce.application.service.discount.DiscountRedemptionReleaser;
import com.ecommerce.persistence.entity.Order;
import com.ecommerce.persistence.entity.enumeration.OrderStatus;
import com.ecommerce.persistence.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationReleaseService {

    /** Shared across instances; only one schedule run holds it at a time. */
    private static final long RESERVATION_RELEASE_LOCK_KEY = 874_231_001L;

    private final OrderRepository orderRepository;
    private final OrderInventoryRestorer inventoryRestorer;
    private final JdbcTemplate jdbcTemplate;
    private final DiscountRedemptionReleaser discountReleaser;

    @Transactional
    public void releaseExpiredReservations() {
        Boolean acquired = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_xact_lock(?)", Boolean.class, RESERVATION_RELEASE_LOCK_KEY);
        if (!Boolean.TRUE.equals(acquired)) {
            return;
        }

        Date now = new Date();
        List<Order> expired = orderRepository.findExpiredReservations(now);
        for (Order order : expired) {
            inventoryRestorer.restore(order);
            discountReleaser.release(order);
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
        }
    }
}
