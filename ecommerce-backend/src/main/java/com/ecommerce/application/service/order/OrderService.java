package com.ecommerce.application.service.order;

import com.ecommerce.application.api.dto.order.OrderResponseDto;
import com.ecommerce.application.api.dto.order.PaymentConfirmRequestDto;
import com.ecommerce.application.api.dto.order.PaymentInitiationResponseDto;
import com.ecommerce.application.api.dto.order.RefundRequestDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.application.service.discount.DiscountRedemptionReleaser;
import com.ecommerce.application.service.payment.PaymentGateway;
import com.ecommerce.application.service.payment.PaymentInitiation;
import com.ecommerce.application.service.payment.PaymentVerification;
import com.ecommerce.persistence.entity.Order;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.Transaction;
import com.ecommerce.persistence.entity.enumeration.OrderStatus;
import com.ecommerce.persistence.entity.enumeration.PaymentMethod;
import com.ecommerce.persistence.entity.enumeration.TransactionType;
import com.ecommerce.persistence.repository.OrderRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Set<OrderStatus> CANCELLABLE = EnumSet.of(OrderStatus.RESERVED, OrderStatus.PAID);
    private static final Set<OrderStatus> CANCELLED = EnumSet.of(
            OrderStatus.CANCEL_BY_USER, OrderStatus.CANCEL_BY_ADMIN);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final PaymentGateway paymentGateway;
    private final OrderInventoryRestorer inventoryRestorer;
    private final DiscountRedemptionReleaser discountReleaser;

    @Transactional(readOnly = true)
    public List<OrderResponseDto> listOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByIdDesc(userId);
        Map<Long, Product> products = loadProducts(orders);
        return orders.stream().map(order -> toDto(order, products)).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrder(Long userId, Long orderId) {
        return toDto(findOwnedOrThrow(userId, orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> listAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByIdDesc();
        Map<Long, Product> products = loadProducts(orders);
        return orders.stream().map(order -> toDto(order, products)).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrderAdmin(Long orderId) {
        return toDto(findOrThrow(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> listRefundableOrders() {
        List<Order> orders = orderRepository.findRefundableOrders();
        Map<Long, Product> products = loadProducts(orders);
        return orders.stream().map(order -> toDto(order, products)).toList();
    }

    @Transactional(readOnly = true)
    public PaymentInitiationResponseDto initiatePayment(Long userId, Long orderId) {
        Order order = findOwnedOrThrow(userId, orderId);
        requireStatus(order, OrderStatus.RESERVED);
        requireReservationActive(order);

        PaymentInitiation initiation = paymentGateway.initiate(order);
        PaymentInitiationResponseDto response = new PaymentInitiationResponseDto();
        response.setPaymentReference(initiation.paymentReference());
        response.setRedirectUrl(initiation.redirectUrl());
        return response;
    }

    @Transactional
    public OrderResponseDto confirmPayment(Long orderId, PaymentConfirmRequestDto requestDto) {
        Order order = findOrThrowForUpdate(orderId);
        requireStatus(order, OrderStatus.RESERVED);
        requireReservationActive(order);

        PaymentVerification verification = paymentGateway.verify(requestDto.getPaymentReference());
        if (!verification.success()) {
            throw new EcommerceException(ECOMErrorType.ORDER_PAYMENT_FAILED);
        }

        order.addTransaction(buildTransaction(
                TransactionType.PAYMENT, order, verification.paymentReference(), null));
        order.setStatus(OrderStatus.PAID);
        order.setReservedUntil(null);
        return toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderResponseDto cancelByUser(Long userId, Long orderId) {
        Order order = findOwnedOrThrowForUpdate(userId, orderId);
        return cancel(order, OrderStatus.CANCEL_BY_USER);
    }

    @Transactional
    public OrderResponseDto cancelByAdmin(Long orderId) {
        Order order = findOrThrowForUpdate(orderId);
        return cancel(order, OrderStatus.CANCEL_BY_ADMIN);
    }

    /**
     * Admin records a manual bank refund after transferring money to the user IBAN.
     */
    @Transactional
    public OrderResponseDto recordRefund(Long orderId, RefundRequestDto requestDto) {
        Order order = findOrThrowForUpdate(orderId);
        if (!CANCELLED.contains(order.getStatus())) {
            throw new EcommerceException(ECOMErrorType.ORDER_INVALID_STATUS);
        }
        boolean hasPayment = order.getTransactions().stream()
                .anyMatch(tx -> tx.getType() == TransactionType.PAYMENT);
        boolean alreadyRefunded = order.getTransactions().stream()
                .anyMatch(tx -> tx.getType() == TransactionType.REFUND);
        if (!hasPayment || alreadyRefunded) {
            throw new EcommerceException(ECOMErrorType.ORDER_INVALID_STATUS);
        }

        order.addTransaction(buildTransaction(
                TransactionType.REFUND, order, requestDto.getReference(), requestDto.getIban()));
        return toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderResponseDto markSending(Long orderId) {
        Order order = findOrThrowForUpdate(orderId);
        requireShippable(order);
        order.setStatus(OrderStatus.SENDING);
        return toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderResponseDto confirmReceived(Long userId, Long orderId) {
        Order order = findOwnedOrThrowForUpdate(userId, orderId);
        requireStatus(order, OrderStatus.SENDING);
        order.setStatus(OrderStatus.RECEIVED);
        return toDto(orderRepository.save(order));
    }

    private OrderResponseDto cancel(Order order, OrderStatus cancelStatus) {
        requireCancellable(order);
        inventoryRestorer.restore(order);
        discountReleaser.release(order);
        order.setStatus(cancelStatus);
        order.setReservedUntil(null);
        return toDto(orderRepository.save(order));
    }

    private OrderResponseDto toDto(Order order) {
        return toDto(order, loadProducts(List.of(order)));
    }

    private OrderResponseDto toDto(Order order, Map<Long, Product> products) {
        OrderResponseDto dto = orderMapper.toResponseDto(order);
        orderMapper.attachMainImages(dto, products);
        return dto;
    }

    private Map<Long, Product> loadProducts(Collection<Order> orders) {
        List<Long> productIds = orders.stream()
                .filter(Objects::nonNull)
                .flatMap(order -> order.getItems().stream())
                .map(item -> item.getProduct().getProductId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private Transaction buildTransaction(TransactionType type, Order order, String reference, String iban) {
        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setAmount(order.getTotalCost());
        transaction.setReference(reference);
        transaction.setIban(iban);
        return transaction;
    }

    private Order findOwnedOrThrow(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.ORDER_NOT_FOUND));
    }

    private Order findOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.ORDER_NOT_FOUND));
    }

    private Order findOwnedOrThrowForUpdate(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.ORDER_NOT_FOUND));
    }

    private Order findOrThrowForUpdate(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.ORDER_NOT_FOUND));
    }

    private void requireStatus(Order order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new EcommerceException(ECOMErrorType.ORDER_INVALID_STATUS);
        }
    }

    /**
     * An order is ready to ship when it is a paid online order, or a cash-on-delivery order still
     * RESERVED (COD is settled in cash on delivery, so it ships without an online payment first).
     */
    private void requireShippable(Order order) {
        boolean shippable = order.getStatus() == OrderStatus.PAID
                || (order.getStatus() == OrderStatus.RESERVED
                        && order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY);
        if (!shippable) {
            throw new EcommerceException(ECOMErrorType.ORDER_INVALID_STATUS);
        }
    }

    private void requireCancellable(Order order) {
        if (!CANCELLABLE.contains(order.getStatus())) {
            throw new EcommerceException(ECOMErrorType.ORDER_INVALID_STATUS);
        }
    }

    private void requireReservationActive(Order order) {
        if (order.getReservedUntil() != null && order.getReservedUntil().before(new Date())) {
            throw new EcommerceException(ECOMErrorType.ORDER_RESERVATION_EXPIRED);
        }
    }
}
