package com.ecommerce.application.service.order;

import com.ecommerce.application.api.dto.order.OrderResponseDto;
import com.ecommerce.application.api.dto.order.PaymentConfirmRequestDto;
import com.ecommerce.application.api.dto.order.PaymentInitiationResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.application.service.payment.PaymentGateway;
import com.ecommerce.application.service.payment.PaymentInitiation;
import com.ecommerce.application.service.payment.PaymentRefund;
import com.ecommerce.application.service.payment.PaymentVerification;
import com.ecommerce.persistence.entity.AppUser;
import com.ecommerce.persistence.entity.Order;
import com.ecommerce.persistence.entity.OrderItem;
import com.ecommerce.persistence.entity.Transaction;
import com.ecommerce.persistence.entity.embeddable.ProductSnapshot;
import com.ecommerce.persistence.entity.enumeration.OrderStatus;
import com.ecommerce.persistence.entity.enumeration.TransactionType;
import com.ecommerce.persistence.repository.AppUserRepository;
import com.ecommerce.persistence.repository.OrderRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
    private AppUserRepository appUserRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PaymentGateway paymentGateway;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, appUserRepository, new OrderMapperImpl(), paymentGateway,
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
    void cancelByUser_from_reserved_restores_inventory_without_refund_or_transaction() {
        Order order = reservedOrder();
        when(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        OrderResponseDto response = orderService.cancelByUser(USER_ID, ORDER_ID);

        assertEquals(OrderStatus.CANCEL_BY_USER, response.getStatus());
        verify(productRepository).incrementInventory(PRODUCT_ID, 2);
        verify(paymentGateway, never()).refund(any(), any());
        assertEquals(0, order.getTransactions().size());
    }

    @Test
    void cancelByUser_from_paid_refunds_and_creates_refund_transaction() {
        Order order = reservedOrder();
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(userWithIban()));
        when(paymentGateway.refund(eq(order), eq(IBAN))).thenReturn(new PaymentRefund(true, "refund-1"));

        OrderResponseDto response = orderService.cancelByUser(USER_ID, ORDER_ID);

        assertEquals(OrderStatus.CANCEL_BY_USER, response.getStatus());
        verify(paymentGateway).refund(order, IBAN);
        verify(productRepository).incrementInventory(PRODUCT_ID, 2);
        assertEquals(1, order.getTransactions().size());
        Transaction tx = order.getTransactions().getFirst();
        assertEquals(TransactionType.REFUND, tx.getType());
        assertEquals("refund-1", tx.getReference());
        assertEquals(IBAN, tx.getIban());
        assertEquals(0, TOTAL.compareTo(tx.getAmount()));
        assertEquals(TransactionType.REFUND, response.getTransactions().getFirst().getType());
    }

    @Test
    void cancelByAdmin_from_paid_refunds_then_restores_inventory() {
        Order order = reservedOrder();
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(userWithIban()));
        when(paymentGateway.refund(eq(order), eq(IBAN))).thenReturn(new PaymentRefund(true, "refund-1"));

        OrderResponseDto response = orderService.cancelByAdmin(ORDER_ID);

        assertEquals(OrderStatus.CANCEL_BY_ADMIN, response.getStatus());
        verify(paymentGateway).refund(order, IBAN);
        verify(productRepository).incrementInventory(PRODUCT_ID, 2);
        assertEquals(TransactionType.REFUND, order.getTransactions().getFirst().getType());
    }

    @Test
    void cancelByUser_from_paid_without_iban_is_rejected() {
        Order order = reservedOrder();
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        AppUser user = new AppUser();
        user.setId(USER_ID);
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> orderService.cancelByUser(USER_ID, ORDER_ID));
        assertEquals(ECOMErrorType.USER_IBAN_REQUIRED, ex.getEcomErrorType());
        verify(productRepository, never()).incrementInventory(anyLong(), anyInt());
        verify(paymentGateway, never()).refund(any(), any());
        assertEquals(0, order.getTransactions().size());
        assertEquals(OrderStatus.PAID, order.getStatus());
    }

    @Test
    void cancelByUser_from_paid_when_refund_fails_keeps_paid_and_stock() {
        Order order = reservedOrder();
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findByIdAndUserIdForUpdate(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(userWithIban()));
        when(paymentGateway.refund(eq(order), eq(IBAN))).thenReturn(new PaymentRefund(false, null));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> orderService.cancelByUser(USER_ID, ORDER_ID));
        assertEquals(ECOMErrorType.ORDER_REFUND_FAILED, ex.getEcomErrorType());
        verify(productRepository, never()).incrementInventory(anyLong(), anyInt());
        assertEquals(0, order.getTransactions().size());
        assertEquals(OrderStatus.PAID, order.getStatus());
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

    private AppUser userWithIban() {
        AppUser user = new AppUser();
        user.setId(USER_ID);
        user.setIban(IBAN);
        return user;
    }

    private PaymentConfirmRequestDto confirmRequest(String reference) {
        PaymentConfirmRequestDto dto = new PaymentConfirmRequestDto();
        dto.setPaymentReference(reference);
        return dto;
    }
}
