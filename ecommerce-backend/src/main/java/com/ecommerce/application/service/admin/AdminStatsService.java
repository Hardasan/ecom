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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Read-only dashboard aggregation. Every figure comes from a {@code COUNT}/{@code SUM} query rather
 * than materialising rows, so the endpoint stays cheap regardless of table size.
 */
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    /** Statuses whose {@code totalCost} counts as realised revenue. */
    private static final Set<OrderStatus> REVENUE_STATUSES = EnumSet.of(
            OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SENDING, OrderStatus.RECEIVED);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductReviewRepository productReviewRepository;
    private final CategoryRepository categoryRepository;
    private final DiscountRepository discountRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponseDto getStats() {
        Map<OrderStatus, Long> ordersByStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            ordersByStatus.put(status, 0L);
        }

        long totalOrders = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        for (Object[] row : orderRepository.aggregateOrderStatus()) {
            OrderStatus status = (OrderStatus) row[0];
            long count = ((Number) row[1]).longValue();
            BigDecimal sum = new BigDecimal(((Number) row[2]).toString());
            ordersByStatus.put(status, count);
            totalOrders += count;
            if (REVENUE_STATUSES.contains(status)) {
                revenue = revenue.add(sum);
            }
        }

        AdminStatsResponseDto dto = new AdminStatsResponseDto();
        dto.setTotalOrders(totalOrders);
        dto.setOrdersByStatus(ordersByStatus);
        dto.setTotalRevenue(revenue);
        dto.setAwaitingShipment(ordersByStatus.get(OrderStatus.PAID));
        dto.setProcessingOrders(ordersByStatus.get(OrderStatus.PROCESSING));
        dto.setReservedOrders(ordersByStatus.get(OrderStatus.RESERVED));
        dto.setRefundableOrders(orderRepository.countRefundableOrders());
        dto.setTotalProducts(productRepository.count());
        dto.setActiveProducts(productRepository.countByStatus(ProductStatus.ACTIVE));
        dto.setOutOfStockProducts(productRepository.countByInventoryCount(0));
        dto.setTotalCategories(categoryRepository.count());
        dto.setTotalDiscounts(discountRepository.count());
        dto.setPendingReviews(productReviewRepository.countByStatus(ReviewStatus.PENDING));
        return dto;
    }
}
