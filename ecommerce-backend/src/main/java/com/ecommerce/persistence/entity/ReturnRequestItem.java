package com.ecommerce.persistence.entity;

import com.ecommerce.persistence.entity.enumeration.ReturnReason;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One line of a {@link ReturnRequest}: which order line is being returned, how many, why, and a
 * snapshot of the product name + unit price at request time (so the request reads correctly even if
 * the catalog later changes).
 */
@Entity
@Table(name = "return_request_item")
@Getter
@Setter
public class ReturnRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "return_request_item_seq")
    @SequenceGenerator(name = "return_request_item_seq", sequenceName = "return_request_item_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    @Column(name = "order_item_id", nullable = false, updatable = false)
    private Long orderItemId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "variant_value", length = 64)
    private String variantValue;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_refund", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineRefund;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 32)
    private ReturnReason reason;
}
