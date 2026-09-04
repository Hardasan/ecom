package com.ecommerce.application.service.returns;

import com.ecommerce.application.api.dto.order.OrderResponseDto;
import com.ecommerce.application.api.dto.returns.CreateReturnItemDto;
import com.ecommerce.application.api.dto.returns.CreateReturnRequestDto;
import com.ecommerce.application.api.dto.returns.ReturnRequestItemResponseDto;
import com.ecommerce.application.api.dto.returns.ReturnRequestResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.application.service.order.OrderMapper;
import com.ecommerce.persistence.entity.Order;
import com.ecommerce.persistence.entity.OrderItem;
import com.ecommerce.persistence.entity.ReturnRequest;
import com.ecommerce.persistence.entity.ReturnRequestItem;
import com.ecommerce.persistence.entity.enumeration.OrderStatus;
import com.ecommerce.persistence.repository.AppUserRepository;
import com.ecommerce.persistence.repository.OrderRepository;
import com.ecommerce.persistence.repository.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Customer returns (مرجوعی). A shopper may open ONE return request per delivered order, within the
 * return window, selecting order lines with a per-line reason. The refundable amount is snapshotted
 * from the order lines (Rial). The money movement itself is not done here — it stays on the existing
 * admin order-refund flow; this only records the request in {@link com.ecommerce.persistence.entity.enumeration.ReturnStatus#REQUESTED}.
 */
@Service
@RequiredArgsConstructor
public class ReturnService {

    /** Days after delivery during which an order can still be returned (design: «تا ۷ روز پس از تحویل»). */
    private static final int RETURN_WINDOW_DAYS = 7;

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final AppUserRepository appUserRepository;
    private final OrderMapper orderMapper;

    /** Orders the shopper can still return: RECEIVED, within the window, and not already requested. */
    @Transactional(readOnly = true)
    public List<OrderResponseDto> listReturnableOrders(Long userId) {
        return orderRepository.findByUserIdOrderByIdDesc(userId).stream()
                .filter(this::isWithinReturnWindow)
                .filter(order -> !returnRequestRepository.existsByOrderId(order.getId()))
                .map(orderMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReturnRequestResponseDto> listReturns(Long userId) {
        return returnRequestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReturnRequestResponseDto getReturn(Long userId, Long id) {
        ReturnRequest request = returnRequestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.RETURN_REQUEST_NOT_FOUND));
        return toDto(request);
    }

    @Transactional
    public ReturnRequestResponseDto createReturn(Long userId, CreateReturnRequestDto dto) {
        Order order = orderRepository.findByIdAndUserId(dto.getOrderId(), userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.ORDER_NOT_FOUND));

        if (!isWithinReturnWindow(order)) {
            throw new EcommerceException(ECOMErrorType.ORDER_NOT_RETURNABLE);
        }
        if (returnRequestRepository.existsByOrderId(order.getId())) {
            throw new EcommerceException(ECOMErrorType.RETURN_ALREADY_REQUESTED);
        }

        Map<Long, OrderItem> orderItems = order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));

        ReturnRequest request = new ReturnRequest();
        request.setOrderId(order.getId());
        request.setUserId(userId);
        request.setNote(dto.getNote());
        request.setIban(dto.getIban() != null ? dto.getIban() : userIban(userId));

        BigDecimal refundTotal = BigDecimal.ZERO;
        for (CreateReturnItemDto line : dto.getItems()) {
            OrderItem orderItem = orderItems.get(line.getOrderItemId());
            if (orderItem == null || line.getQuantity() > orderItem.getQuantity()) {
                throw new EcommerceException(ECOMErrorType.RETURN_ITEM_INVALID);
            }
            BigDecimal unit = effectiveUnitPrice(orderItem);
            BigDecimal lineRefund = unit.multiply(BigDecimal.valueOf(line.getQuantity()));

            ReturnRequestItem item = new ReturnRequestItem();
            item.setOrderItemId(orderItem.getId());
            item.setProductName(orderItem.getProduct().getProductName());
            item.setVariantValue(orderItem.getVariantValue());
            item.setQuantity(line.getQuantity());
            item.setUnitPrice(unit);
            item.setLineRefund(lineRefund);
            item.setReason(line.getReason());
            request.addItem(item);

            refundTotal = refundTotal.add(lineRefund);
        }
        request.setRefundAmount(refundTotal);

        return toDto(returnRequestRepository.save(request));
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private boolean isWithinReturnWindow(Order order) {
        if (order.getStatus() != OrderStatus.RECEIVED || order.getDeliveredAt() == null) {
            return false;
        }
        Duration sinceDelivery = Duration.between(order.getDeliveredAt().toInstant(), new Date().toInstant());
        return sinceDelivery.toDays() <= RETURN_WINDOW_DAYS;
    }

    /** The price actually paid for a line: the discounted price when set, else the unit price. */
    private BigDecimal effectiveUnitPrice(OrderItem item) {
        BigDecimal discount = item.getDiscountPrice();
        return discount != null && discount.signum() > 0 ? discount : item.getUnitPrice();
    }

    private String userIban(Long userId) {
        return appUserRepository.findById(userId).map(user -> user.getIban()).orElse(null);
    }

    private ReturnRequestResponseDto toDto(ReturnRequest request) {
        ReturnRequestResponseDto dto = new ReturnRequestResponseDto();
        dto.setId(request.getId());
        dto.setOrderId(request.getOrderId());
        dto.setStatus(request.getStatus());
        dto.setRefundAmount(request.getRefundAmount());
        dto.setIban(request.getIban());
        dto.setNote(request.getNote());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setUpdatedAt(request.getUpdatedAt());
        List<ReturnRequestItemResponseDto> items = new ArrayList<>();
        for (ReturnRequestItem item : request.getItems()) {
            ReturnRequestItemResponseDto itemDto = new ReturnRequestItemResponseDto();
            itemDto.setOrderItemId(item.getOrderItemId());
            itemDto.setProductName(item.getProductName());
            itemDto.setVariantValue(item.getVariantValue());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPrice());
            itemDto.setLineRefund(item.getLineRefund());
            itemDto.setReason(item.getReason());
            items.add(itemDto);
        }
        dto.setItems(items);
        return dto;
    }
}
