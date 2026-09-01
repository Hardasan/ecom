package com.ecommerce.application.api.dto.order;

import com.ecommerce.persistence.entity.enumeration.OrderStatus;
import com.ecommerce.persistence.entity.enumeration.PaymentMethod;
import com.ecommerce.persistence.entity.enumeration.Province;
import com.ecommerce.persistence.entity.enumeration.ShippingZone;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class OrderResponseDto {

    private Long id;

    private Long userId;

    private OrderStatus status;

    private PaymentMethod paymentMethod;

    // Shipping address snapshot
    private String recipientFirstName;

    private String recipientLastName;

    private String recipientMobile;

    private String recipientNationalId;

    private Province province;

    private String city;

    private String postalCode;

    private String addressLine;

    private String plaque;

    private String unit;

    private List<OrderItemResponseDto> items;

    private List<TransactionResponseDto> transactions;

    private BigDecimal itemsCost;

    private BigDecimal shippingCost;

    private BigDecimal totalCost;

    // Applied discount code snapshot (null / 0 when no code was used).
    private String discountCode;

    private BigDecimal discountAmount;

    private Integer totalWeightGram;

    private ShippingZone shippingZone;

    private Date reservedUntil;

    private Date createdAt;

    private Date updatedAt;
}
