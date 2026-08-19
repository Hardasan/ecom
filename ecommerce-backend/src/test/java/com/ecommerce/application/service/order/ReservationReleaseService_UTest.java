package com.ecommerce.application.service.order;

import com.ecommerce.application.service.discount.DiscountRedemptionReleaser;
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
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationReleaseService_UTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private DiscountRedemptionReleaser discountReleaser;

    private ReservationReleaseService releaseService;

    @BeforeEach
    void setUp() {
        releaseService = new ReservationReleaseService(
                orderRepository, new OrderInventoryRestorer(productRepository), jdbcTemplate, discountReleaser);
    }

    @Test
    void release_expired_orders_increments_inventory_and_sets_failed_status() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), anyLong())).thenReturn(true);
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
        verify(discountReleaser).release(order);
        verify(orderRepository).save(order);
        verify(orderRepository).findExpiredReservations(any(Date.class));
        assertEquals(OrderStatus.FAILED, order.getStatus());
    }

    @Test
    void no_expired_reservations_does_nothing() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), anyLong())).thenReturn(true);
        when(orderRepository.findExpiredReservations(any(Date.class))).thenReturn(List.of());

        releaseService.releaseExpiredReservations();

        verify(productRepository, never()).incrementInventory(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void skips_work_when_another_instance_holds_the_lock() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), anyLong())).thenReturn(false);

        releaseService.releaseExpiredReservations();

        verify(orderRepository, never()).findExpiredReservations(any());
        verify(productRepository, never()).incrementInventory(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
    }
}
