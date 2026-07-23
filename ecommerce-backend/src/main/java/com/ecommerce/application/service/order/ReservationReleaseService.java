package com.ecommerce.application.service.order;

import com.ecommerce.persistence.entity.Order;
import com.ecommerce.persistence.entity.enumeration.OrderStatus;
import com.ecommerce.persistence.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationReleaseService {

    private final OrderRepository orderRepository;
    private final OrderInventoryRestorer inventoryRestorer;

    @Transactional
    public void releaseExpiredReservations() {
        Date now = new Date();
        List<Order> expired = orderRepository.findExpiredReservations(now);
        for (Order order : expired) {
            inventoryRestorer.restore(order);
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
        }
    }
}
