package com.ecommerce.application.service.returns;

import com.ecommerce.application.api.dto.returns.ReturnRefundRequestDto;
import com.ecommerce.application.api.dto.returns.ReturnRequestResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.application.service.order.OrderMapper;
import com.ecommerce.persistence.entity.Order;
import com.ecommerce.persistence.entity.OrderItem;
import com.ecommerce.persistence.entity.ReturnRequest;
import com.ecommerce.persistence.entity.ReturnRequestItem;
import com.ecommerce.persistence.entity.Transaction;
import com.ecommerce.persistence.entity.embeddable.ProductSnapshot;
import com.ecommerce.persistence.entity.enumeration.ReturnReason;
import com.ecommerce.persistence.entity.enumeration.ReturnStatus;
import com.ecommerce.persistence.entity.enumeration.TransactionType;
import com.ecommerce.persistence.repository.AppUserRepository;
import com.ecommerce.persistence.repository.OrderRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import com.ecommerce.persistence.repository.ReturnRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit coverage for the returns moderation state machine of {@link ReturnService}: the admin
 * approve/reject, the warehouse accept (which restocks) / reject, and the admin refund (which posts
 * the REFUND transaction), plus the status guards that gate each transition.
 * <p>
 * Lifecycle: REQUESTED → APPROVED → RECEIVED → REFUNDED (or → REJECTED).
 */
@ExtendWith(MockitoExtension.class)
class ReturnService_adminModerationUTest {

    private static final Long RETURN_ID = 5L;
    private static final Long ORDER_ID = 42L;
    private static final Long USER_ID = 7L;
    private static final String IBAN = "IR062960000000100324200001";

    @Mock
    private ReturnRequestRepository returnRequestRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderMapper orderMapper;

    private ReturnService returnService;

    @BeforeEach
    void setUp() {
        returnService = new ReturnService(
                returnRequestRepository, orderRepository, appUserRepository, productRepository, orderMapper);
        lenient().when(returnRequestRepository.save(any(ReturnRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(appUserRepository.findById(any())).thenReturn(Optional.empty());
    }

    // ---- admin approve / reject ------------------------------------------------------------------

    @Test
    void approve_moves_a_pending_request_to_approved() {
        ReturnRequest request = requestWith(ReturnStatus.REQUESTED);
        when(returnRequestRepository.findByIdForUpdate(RETURN_ID)).thenReturn(Optional.of(request));

        ReturnRequestResponseDto dto = returnService.approve(RETURN_ID);

        assertEquals(ReturnStatus.APPROVED, dto.getStatus());
        assertEquals(ReturnStatus.APPROVED, request.getStatus());
    }

    @Test
    void reject_moves_a_pending_request_to_rejected() {
        ReturnRequest request = requestWith(ReturnStatus.REQUESTED);
        when(returnRequestRepository.findByIdForUpdate(RETURN_ID)).thenReturn(Optional.of(request));

        ReturnRequestResponseDto dto = returnService.reject(RETURN_ID);

        assertEquals(ReturnStatus.REJECTED, dto.getStatus());
    }

    @Test
    void approve_on_a_non_pending_request_is_rejected() {
        when(returnRequestRepository.findByIdForUpdate(RETURN_ID))
                .thenReturn(Optional.of(requestWith(ReturnStatus.APPROVED)));

        EcommerceException ex = assertThrows(EcommerceException.class, () -> returnService.approve(RETURN_ID));
        assertEquals(ECOMErrorType.RETURN_INVALID_STATUS, ex.getEcomErrorType());
    }

    // ---- warehouse accept (restock) / reject -----------------------------------------------------

    @Test
    void warehouse_accept_restocks_each_line_and_marks_received() {
        ReturnRequest request = requestWith(ReturnStatus.APPROVED);
        request.addItem(returnItem(1L, 2)); // 2 of order item #1
        request.addItem(returnItem(2L, 1)); // 1 of order item #2
        when(returnRequestRepository.findByIdForUpdate(RETURN_ID)).thenReturn(Optional.of(request));

        Order order = new Order();
        order.setId(ORDER_ID);
        order.addItem(orderItem(1L, 100L));
        order.addItem(orderItem(2L, 200L));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        ReturnRequestResponseDto dto = returnService.receiveByWarehouse(RETURN_ID);

        verify(productRepository).incrementInventory(100L, 2);
        verify(productRepository).incrementInventory(200L, 1);
        assertEquals(ReturnStatus.RECEIVED, dto.getStatus());
        // Accepting the goods is not the money step — no transaction is posted here.
        assertTrue(order.getTransactions().isEmpty());
    }

    @Test
    void warehouse_accept_on_a_non_approved_request_is_rejected() {
        when(returnRequestRepository.findByIdForUpdate(RETURN_ID))
                .thenReturn(Optional.of(requestWith(ReturnStatus.REQUESTED)));

        EcommerceException ex =
                assertThrows(EcommerceException.class, () -> returnService.receiveByWarehouse(RETURN_ID));
        assertEquals(ECOMErrorType.RETURN_INVALID_STATUS, ex.getEcomErrorType());
        verifyNoInteractions(productRepository);
    }

    @Test
    void warehouse_reject_moves_an_approved_request_to_rejected() {
        ReturnRequest request = requestWith(ReturnStatus.APPROVED);
        when(returnRequestRepository.findByIdForUpdate(RETURN_ID)).thenReturn(Optional.of(request));

        ReturnRequestResponseDto dto = returnService.rejectByWarehouse(RETURN_ID);

        assertEquals(ReturnStatus.REJECTED, dto.getStatus());
        verifyNoInteractions(productRepository);
    }

    // ---- admin refund ----------------------------------------------------------------------------

    @Test
    void refund_before_warehouse_receipt_is_rejected() {
        // APPROVED but not yet RECEIVED — the money cannot move before the goods are back.
        when(returnRequestRepository.findByIdForUpdate(RETURN_ID))
                .thenReturn(Optional.of(requestWith(ReturnStatus.APPROVED)));

        ReturnRefundRequestDto dto = new ReturnRefundRequestDto();
        dto.setReference("BANK-1");

        EcommerceException ex = assertThrows(EcommerceException.class, () -> returnService.refund(RETURN_ID, dto));
        assertEquals(ECOMErrorType.RETURN_INVALID_STATUS, ex.getEcomErrorType());
    }

    @Test
    void refund_posts_a_refund_transaction_and_marks_refunded_without_restocking() {
        ReturnRequest request = requestWith(ReturnStatus.RECEIVED);
        request.setRefundAmount(BigDecimal.valueOf(300));
        when(returnRequestRepository.findByIdForUpdate(RETURN_ID)).thenReturn(Optional.of(request));

        Order order = new Order();
        order.setId(ORDER_ID);
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        ReturnRefundRequestDto dto = new ReturnRefundRequestDto();
        dto.setReference("BANK-1");

        ReturnRequestResponseDto result = returnService.refund(RETURN_ID, dto);

        // One REFUND transaction for the snapshotted amount + reference + شبا; restock already happened
        // at warehouse acceptance, so refund must NOT touch inventory again.
        assertEquals(1, order.getTransactions().size());
        Transaction tx = order.getTransactions().get(0);
        assertEquals(TransactionType.REFUND, tx.getType());
        assertEquals(0, BigDecimal.valueOf(300).compareTo(tx.getAmount()));
        assertEquals("BANK-1", tx.getReference());
        assertEquals(IBAN, tx.getIban());
        verifyNoInteractions(productRepository);

        assertEquals(ReturnStatus.REFUNDED, result.getStatus());
        assertEquals("BANK-1", result.getRefundReference());
    }

    // ---- fixtures --------------------------------------------------------------------------------

    private ReturnRequest requestWith(ReturnStatus status) {
        ReturnRequest request = new ReturnRequest();
        request.setId(RETURN_ID);
        request.setOrderId(ORDER_ID);
        request.setUserId(USER_ID);
        request.setStatus(status);
        request.setIban(IBAN);
        return request;
    }

    private ReturnRequestItem returnItem(long orderItemId, int quantity) {
        ReturnRequestItem item = new ReturnRequestItem();
        item.setOrderItemId(orderItemId);
        item.setQuantity(quantity);
        item.setUnitPrice(BigDecimal.valueOf(100));
        item.setLineRefund(BigDecimal.valueOf(100L * quantity));
        item.setReason(ReturnReason.DEFECTIVE);
        return item;
    }

    private OrderItem orderItem(long id, long productId) {
        OrderItem item = new OrderItem();
        item.setId(id);
        ProductSnapshot snapshot = new ProductSnapshot();
        snapshot.setProductId(productId);
        item.setProduct(snapshot);
        item.setQuantity(1);
        return item;
    }
}
