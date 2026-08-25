package com.ecommerce.application.service.admin;

import com.ecommerce.application.api.dto.admin.AdminStatsResponseDto;
import com.ecommerce.persistence.entity.enumeration.OrderStatus;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.ReviewStatus;
import com.ecommerce.persistence.repository.CategoryRepository;
import com.ecommerce.persistence.repository.DiscountRepository;
import com.ecommerce.persistence.repository.OrderRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import com.ecommerce.persistence.repository.ProductReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceUTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductReviewRepository productReviewRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private DiscountRepository discountRepository;

    private AdminStatsService service;

    @BeforeEach
    void setUp() {
        service = new AdminStatsService(orderRepository, productRepository, productReviewRepository,
                categoryRepository, discountRepository);
    }

    @Test
    void computes_totals_revenue_and_zero_fills_absent_statuses() {
        when(orderRepository.aggregateOrderStatus()).thenReturn(List.of(
                new Object[]{OrderStatus.PAID, 3L, new BigDecimal("300")},
                new Object[]{OrderStatus.RESERVED, 2L, new BigDecimal("150")},
                new Object[]{OrderStatus.RECEIVED, 1L, new BigDecimal("100")},
                new Object[]{OrderStatus.CANCEL_BY_USER, 4L, new BigDecimal("400")}));
        when(orderRepository.countRefundableOrders()).thenReturn(2L);
        when(productRepository.count()).thenReturn(10L);
        when(productRepository.countByStatus(ProductStatus.ACTIVE)).thenReturn(7L);
        when(productRepository.countByInventoryCount(0)).thenReturn(3L);
        when(categoryRepository.count()).thenReturn(4L);
        when(discountRepository.count()).thenReturn(5L);
        when(productReviewRepository.countByStatus(ReviewStatus.PENDING)).thenReturn(6L);

        AdminStatsResponseDto dto = service.getStats();

        assertEquals(10, dto.getTotalOrders());
        // Revenue counts PAID + SENDING + RECEIVED only: 300 + 0 + 100.
        assertEquals(0, dto.getTotalRevenue().compareTo(new BigDecimal("400")));
        assertEquals(3, dto.getAwaitingShipment());
        assertEquals(2, dto.getReservedOrders());
        assertEquals(2, dto.getRefundableOrders());
        assertEquals(OrderStatus.values().length, dto.getOrdersByStatus().size());
        assertEquals(3L, dto.getOrdersByStatus().get(OrderStatus.PAID));
        assertEquals(4L, dto.getOrdersByStatus().get(OrderStatus.CANCEL_BY_USER));
        assertEquals(0L, dto.getOrdersByStatus().get(OrderStatus.FAILED));
        assertEquals(0L, dto.getOrdersByStatus().get(OrderStatus.SENDING));
        assertEquals(0L, dto.getOrdersByStatus().get(OrderStatus.CANCEL_BY_ADMIN));
        assertEquals(10, dto.getTotalProducts());
        assertEquals(7, dto.getActiveProducts());
        assertEquals(3, dto.getOutOfStockProducts());
        assertEquals(4, dto.getTotalCategories());
        assertEquals(5, dto.getTotalDiscounts());
        assertEquals(6, dto.getPendingReviews());
    }

    @Test
    void handles_an_empty_store() {
        when(orderRepository.aggregateOrderStatus()).thenReturn(List.of());
        when(orderRepository.countRefundableOrders()).thenReturn(0L);

        AdminStatsResponseDto dto = service.getStats();

        assertEquals(0, dto.getTotalOrders());
        assertEquals(0, dto.getTotalRevenue().compareTo(BigDecimal.ZERO));
        assertEquals(OrderStatus.values().length, dto.getOrdersByStatus().size());
        assertEquals(true, dto.getOrdersByStatus().values().stream().allMatch(v -> v == 0L));
    }
}
