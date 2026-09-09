package com.ecommerce.application.api.dto.returns;

import com.ecommerce.persistence.entity.enumeration.ReturnStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class ReturnRequestResponseDto {

    private Long id;

    private Long orderId;

    private ReturnStatus status;

    private BigDecimal refundAmount;

    private String iban;

    private String note;

    // Bank transfer reference, set once the refund is paid (status REFUNDED).
    private String refundReference;

    // Buyer identity — populated only on the admin views (null on the shopper's own list).
    private String customerName;

    private String mobile;

    private List<ReturnRequestItemResponseDto> items;

    private Date createdAt;

    private Date updatedAt;
}
