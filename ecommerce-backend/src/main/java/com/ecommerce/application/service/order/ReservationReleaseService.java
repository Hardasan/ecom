package com.ecommerce.application.service.order;

import com.ecommerce.persistence.entity.Order;
import com.ecommerce.persistence.entity.OrderItem;
import com.ecommerce.persistence.entity.enumeration.OrderStatus;
import com.ecommerce.persistence.repository.OrderRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.checkout.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ReservationReleaseService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Scheduled(fixedDelayString = "${app.checkout.reservation-release-interval:60000}")
    @Transactional
    public void releaseExpiredReservations() {
        Date now = new Date();
        List<Order> expired = orderRepository.findExpiredReservations(now);
        for (Order order : expired) {
            for (OrderItem item : order.getItems()) {
                productRepository.incrementInventory(
                        item.getProduct().getProductId(), item.getQuantity());
            }
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);
        }
    }
}
