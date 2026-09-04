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

    private List<ReturnRequestItemResponseDto> items;

    private Date createdAt;

    private Date updatedAt;
}
