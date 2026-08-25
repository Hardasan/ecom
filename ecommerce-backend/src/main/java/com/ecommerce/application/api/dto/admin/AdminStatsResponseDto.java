package com.ecommerce.application.api.dto.admin;

import com.ecommerce.persistence.entity.enumeration.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Aggregated figures for the admin dashboard, all computed with SQL aggregates so the dashboard
 * never has to pull whole tables to the client. Money fields are in Rial (the storage unit); the
 * client converts to Toman for display.
 */
@Getter
@Setter
public class AdminStatsResponseDto {

    private long totalOrders;

    /** Order count per status, zero-filled for every {@link OrderStatus}. */
    private Map<OrderStatus, Long> ordersByStatus;

    /** Sum of {@code totalCost} over PAID / SENDING / RECEIVED orders. */
    private BigDecimal totalRevenue;

    /** PAID orders awaiting an admin to mark them SENDING. */
    private long awaitingShipment;

    /** Orders still holding stock in RESERVED. */
    private long reservedOrders;

    /** Cancelled-but-paid orders with no refund recorded yet. */
    private long refundableOrders;

    private long totalProducts;
    private long activeProducts;
    private long outOfStockProducts;

    private long totalCategories;
    private long totalDiscounts;

    /** Reviews awaiting moderation. */
    private long pendingReviews;
}
