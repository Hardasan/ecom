package com.ecommerce.application.service.order;

import com.ecommerce.persistence.entity.Order;
import com.ecommerce.persistence.entity.OrderItem;
import com.ecommerce.persistence.entity.embeddable.ProductSnapshot;
import com.ecommerce.persistence.entity.enumeration.OrderStatus;
import com.ecommerce.persistence.repository.OrderRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationReleaseService_UTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;

    private ReservationReleaseService releaseService;

    @BeforeEach
    void setUp() {
        releaseService = new ReservationReleaseService(orderRepository, new OrderInventoryRestorer(productRepository));
    }

    @Test
    void release_expired_orders_increments_inventory_and_sets_failed_status() {
        OrderItem item = new OrderItem();
        ProductSnapshot snapshot = new ProductSnapshot();
        snapshot.setProductId(10L);
        item.setProduct(snapshot);
        item.setQuantity(3);
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.RESERVED);
        order.addItem(item);
        when(orderRepository.findExpiredReservations(any(Date.class))).thenReturn(List.of(order));

        releaseService.releaseExpiredReservations();

        verify(productRepository).incrementInventory(10L, 3);
        verify(orderRepository).save(order);
        verify(orderRepository).findExpiredReservations(any(Date.class));
        assertEquals(OrderStatus.FAILED, order.getStatus());
    }

    @Test
    void no_expired_reservations_does_nothing() {
        when(orderRepository.findExpiredReservations(any(Date.class))).thenReturn(List.of());

        releaseService.releaseExpiredReservations();

        verify(productRepository, never()).incrementInventory(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
    }
}
