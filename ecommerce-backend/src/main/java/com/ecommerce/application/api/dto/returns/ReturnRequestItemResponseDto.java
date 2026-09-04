package com.ecommerce.application.api.dto.returns;

import com.ecommerce.persistence.entity.enumeration.ReturnReason;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReturnRequestItemResponseDto {

    private Long orderItemId;

    private String productName;

    private String variantValue;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal lineRefund;

    private ReturnReason reason;
}
