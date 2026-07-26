package com.ecommerce.application.service.order;

import com.ecommerce.application.api.dto.order.OrderResponseDto;
import com.ecommerce.application.api.dto.order.PaymentConfirmRequestDto;
import com.ecommerce.application.api.dto.order.PaymentInitiationResponseDto;
import com.ecommerce.application.api.dto.order.RefundRequestDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.application.service.payment.PaymentGateway;
import com.ecommerce.application.service.payment.PaymentInitiation;
import com.ecommerce.application.service.payment.PaymentVerification;
import com.ecommerce.persistence.entity.Order;
import com.ecommerce.persistence.entity.OrderItem;
import com.ecommerce.persistence.entity.Transaction;
import com.ecommerce.persistence.entity.embeddable.ProductSnapshot;
import com.ecommerce.persistence.entity.enumeration.OrderStatus;
import com.ecommerce.persistence.entity.enumeration.TransactionType;
import com.ecommerce.persistence.repository.OrderRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderService_UTest {

    private static final Long USER_ID = 7L;
    private static final Long ORDER_ID = 42L;
    private static final Long PRODUCT_ID = 100L;
    private static final String IBAN = "IR062960000000100324200001";
    private static final BigDecimal TOTAL = BigDecimal.valueOf(200);

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PaymentGateway paymentGateway;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, new OrderMapperImpl(), paymentGateway,
                new OrderInventoryRestorer(productRepository));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- pay / confirm ---

    @Test
    void initiatePayment_returns_gateway_result_for_reserved_order() {
        Order order = reservedOrder();
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentGateway.initiate(order)).thenReturn(new PaymentInitiation("ref-1", "https://pay/ref-1"));

        PaymentInitiationResponseDto response = orderService.initiatePayment(USER_ID, ORDER_ID);

        assertEquals("ref-1", response.getPaymentReference());
        assertEquals("https://pay/ref-1", response.getRedirectUrl());
    }

    @Test
    void initiatePayment_rejects_non_reserved() {
        Order order = reservedOrder();
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> orderService.initiatePayment(USER_ID, ORDER_ID));
        assertEquals(ECOMErrorType.ORDER_INVALID_STATUS, ex.getEcomErrorType());
    }

    @Test
    void initiatePayment_rejects_expired_reservation() {
        Order order = reservedOrder();
        order.setReservedUntil(new Date(System.currentTimeMillis() - 60_000));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> orderService.initiatePayment(USER_ID, ORDER_ID));
        assertEquals(ECOMErrorType.ORDER_RESERVATION_EXPIRED, ex.getEcomErrorType());
    }

    @Test
    void confirmPayment_marks_paid_and_creates_payment_transaction() {
        Order order = reservedOrder();
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentGateway.verify("ref-1")).thenReturn(new PaymentVerification(true, "ref-1"));

        OrderResponseDto response = orderService.confirmPayment(ORDER_ID, confirmRequest("ref-1"));

        assertEquals(OrderStatus.PAID, response.getStatus());
        assertNull(response.getReservedUntil());
        assertEquals(1, order.getTransactions().size());
        Transaction tx = order.getTransactions().getFirst();
        assertEquals(TransactionType.PAYMENT, tx.getType());
        assertEquals(0, TOTAL.compareTo(tx.getAmount()));
        assertEquals("ref-1", tx.getReference());
        assertNull(tx.getIban());
        assertEquals(1, response.getTransactions().size());
        assertEquals(TransactionType.PAYMENT, response.getTransactions().getFirst().getType());
    }

    @Test
    void confirmPayment_rejects_failed_verification_without_transaction() {
        Order order = reservedOrder();
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentGateway.verify("ref-1")).thenReturn(new PaymentVerification(false, "ref-1"));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> orderService.confirmPayment(ORDER_ID, confirmRequest("ref-1")));
        assertEquals(ECOMErrorType.ORDER_PAYMENT_FAILED, ex.getEcomErrorType());
        assertEquals(0, order.getTransactions().size());
        assertEquals(OrderStatus.RESERVED, order.getStatus());
    }

    @Test
    void confirmPayment_rejects_non_reserved_without_transaction() {
        Order order = reservedOrder();
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> orderService.confirmPayment(ORDER_ID, confirmRequest("ref-1")));
        assertEquals(ECOMErrorType.ORDER_INVALID_STATUS, ex.getEcomErrorType());
        verify(paymentGateway, never()).verify(any());
        assertEquals(0, order.getTransactions().size());
    }

    @Test
    void confirmPayment_rejects_expired_reservation_without_transaction() {
        Order order = reservedOrder();
        order.setReservedUntil(new Date(System.currentTimeMillis() - 60_000));
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> orderService.confirmPayment(ORDER_ID, confirmRequest("ref-1")));
        assertEquals(ECOMErrorType.ORDER_RESERVATION_EXPIRED, ex.getEcomErrorType());
        verify(paymentGateway, never()).verify(any());
        assertEquals(0, order.getTransactions().size());
    }

    // --- cancel / refund ---

    @Test
    void cancelByUser_from_reserved_restores_inventory_without_transaction() {
        Order order = reservedOrder();
        when(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        OrderResponseDto response = orderService.cancelByUser(USER_ID, ORDER_ID);

        assertEquals(OrderStatus.CANCEL_BY_USER, response.getStatus());
        verify(productRepository).incrementInventory(PRODUCT_ID, 2);
        assertEquals(0, order.getTransactions().size());
    }

    @Test
    void cancelByUser_from_paid_restores_inventory_without_creating_refund() {
        Order order = paidOrderWithPayment();
        when(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        OrderResponseDto response = orderService.cancelByUser(USER_ID, ORDER_ID);

        assertEquals(OrderStatus.CANCEL_BY_USER, response.getStatus());
        verify(productRepository).incrementInventory(PRODUCT_ID, 2);
        assertEquals(1, order.getTransactions().size());
        assertEquals(TransactionType.PAYMENT, order.getTransactions().getFirst().getType());
    }

    @Test
    void cancelByAdmin_from_paid_restores_inventory_without_creating_refund() {
        Order order = paidOrderWithPayment();
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        OrderResponseDto response = orderService.cancelByAdmin(ORDER_ID);

        assertEquals(OrderStatus.CANCEL_BY_ADMIN, response.getStatus());
        verify(productRepository).incrementInventory(PRODUCT_ID, 2);
        assertEquals(1, order.getTransactions().size());
    }

    @Test
    void listRefundableOrders_maps_repository_result() {
        Order order = paidOrderWithPayment();
        order.setStatus(OrderStatus.CANCEL_BY_USER);
        when(orderRepository.findRefundableOrders()).thenReturn(List.of(order));

        List<OrderResponseDto> result = orderService.listRefundableOrders();

        assertEquals(1, result.size());
        assertEquals(ORDER_ID, result.getFirst().getId());
    }

    @Test
    void recordRefund_adds_refund_transaction() {
        Order order = paidOrderWithPayment();
        order.setStatus(OrderStatus.CANCEL_BY_USER);
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        OrderResponseDto response = orderService.recordRefund(ORDER_ID, refundRequest("bank-ref-1"));

        assertEquals(2, order.getTransactions().size());
        Transaction tx = order.getTransactions().get(1);
        assertEquals(TransactionType.REFUND, tx.getType());
        assertEquals("bank-ref-1", tx.getReference());
        assertEquals(IBAN, tx.getIban());
        assertEquals(0, TOTAL.compareTo(tx.getAmount()));
        assertEquals(2, response.getTransactions().size());
    }

    @Test
    void recordRefund_rejects_when_not_cancelled() {
        Order order = paidOrderWithPayment();
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> orderService.recordRefund(ORDER_ID, refundRequest("bank-ref-1")));
        assertEquals(ECOMErrorType.ORDER_INVALID_STATUS, ex.getEcomErrorType());
        assertEquals(1, order.getTransactions().size());
    }

    @Test
    void recordRefund_rejects_when_already_refunded() {
        Order order = paidOrderWithPayment();
        order.setStatus(OrderStatus.CANCEL_BY_ADMIN);
        Transaction refund = new Transaction();
        refund.setType(TransactionType.REFUND);
        refund.setAmount(TOTAL);
        refund.setReference("existing");
        refund.setIban(IBAN);
        order.addTransaction(refund);
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> orderService.recordRefund(ORDER_ID, refundRequest("bank-ref-1")));
        assertEquals(ECOMErrorType.ORDER_INVALID_STATUS, ex.getEcomErrorType());
    }

    @Test
    void cancelByUser_rejects_sending() {
        Order order = reservedOrder();
        order.setStatus(OrderStatus.SENDING);
        when(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> orderService.cancelByUser(USER_ID, ORDER_ID));
        assertEquals(ECOMErrorType.ORDER_INVALID_STATUS, ex.getEcomErrorType());
        verify(productRepository, never()).incrementInventory(anyLong(), anyInt());
    }

    @Test
    void cancelByUser_rejects_received() {
        Order order = reservedOrder();
        order.setStatus(OrderStatus.RECEIVED);
        when(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> orderService.cancelByUser(USER_ID, ORDER_ID));
        assertEquals(ECOMErrorType.ORDER_INVALID_STATUS, ex.getEcomErrorType());
    }

    @Test
    void cancelByUser_rejects_failed() {
        Order order = reservedOrder();
        order.setStatus(OrderStatus.FAILED);
        when(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> orderService.cancelByUser(USER_ID, ORDER_ID));
        assertEquals(ECOMErrorType.ORDER_INVALID_STATUS, ex.getEcomErrorType());
    }

    // --- send / receive ---

    @Test
    void markSending_from_paid() {
        Order order = reservedOrder();
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        OrderResponseDto response = orderService.markSending(ORDER_ID);

        assertEquals(OrderStatus.SENDING, response.getStatus());
    }

    @Test
    void markSending_rejects_reserved() {
        Order order = reservedOrder();
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> orderService.markSending(ORDER_ID));
        assertEquals(ECOMErrorType.ORDER_INVALID_STATUS, ex.getEcomErrorType());
    }

    @Test
    void confirmReceived_from_sending() {
        Order order = reservedOrder();
        order.setStatus(OrderStatus.SENDING);
        when(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        OrderResponseDto response = orderService.confirmReceived(USER_ID, ORDER_ID);

        assertEquals(OrderStatus.RECEIVED, response.getStatus());
    }

    @Test
    void confirmReceived_rejects_paid() {
        Order order = reservedOrder();
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> orderService.confirmReceived(USER_ID, ORDER_ID));
        assertEquals(ECOMErrorType.ORDER_INVALID_STATUS, ex.getEcomErrorType());
    }

    @Test
    void getOrder_not_found_for_wrong_owner() {
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.empty());

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> orderService.getOrder(USER_ID, ORDER_ID));
        assertEquals(ECOMErrorType.ORDER_NOT_FOUND, ex.getEcomErrorType());
    }

    private Order reservedOrder() {
        OrderItem item = new OrderItem();
        ProductSnapshot snapshot = new ProductSnapshot();
        snapshot.setProductId(PRODUCT_ID);
        snapshot.setProductName("Laptop");
        snapshot.setProductCode("1-1");
        item.setProduct(snapshot);
        item.setQuantity(2);

        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.RESERVED);
        order.setTotalCost(TOTAL);
        order.setReservedUntil(new Date(System.currentTimeMillis() + 30 * 60_000));
        order.addItem(item);
        return order;
    }

    private Order paidOrderWithPayment() {
        Order order = reservedOrder();
        order.setStatus(OrderStatus.PAID);
        order.setReservedUntil(null);
        Transaction payment = new Transaction();
        payment.setType(TransactionType.PAYMENT);
        payment.setAmount(TOTAL);
        payment.setReference("pay-ref-1");
        order.addTransaction(payment);
        return order;
    }

    private PaymentConfirmRequestDto confirmRequest(String reference) {
        PaymentConfirmRequestDto dto = new PaymentConfirmRequestDto();
        dto.setPaymentReference(reference);
        return dto;
    }

    private RefundRequestDto refundRequest(String reference) {
        RefundRequestDto dto = new RefundRequestDto();
        dto.setReference(reference);
        dto.setIban(IBAN);
        return dto;
    }
}
