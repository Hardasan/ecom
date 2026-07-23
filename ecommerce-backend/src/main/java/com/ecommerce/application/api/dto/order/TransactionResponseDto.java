package com.ecommerce.application.api.dto.order;

import com.ecommerce.persistence.entity.enumeration.TransactionType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class TransactionResponseDto {

    private Long id;

    private TransactionType type;

    private BigDecimal amount;

    private String reference;

    private String iban;

    private Date createdAt;
}
