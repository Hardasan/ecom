package com.ecommerce.persistence.entity;

import com.ecommerce.persistence.entity.enumeration.ReturnStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A customer return (مرجوعی) request for one delivered order. Holds the requested lines, the derived
 * refund amount (Rial, snapshotted at request time), and the شبا the shopper wants the refund paid to.
 * One request per order (see the unique index in V1.28); the admin refund itself stays on the order.
 */
@Entity
@Table(name = "return_request")
@Getter
@Setter
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "return_request_seq")
    @SequenceGenerator(name = "return_request_seq", sequenceName = "return_request_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReturnStatus status = ReturnStatus.REQUESTED;

    // Total refundable amount for the returned lines, in Rial (matches order money scale).
    @Column(name = "refund_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "iban", length = 26)
    private String iban;

    @Column(name = "note", length = 1000)
    private String note;

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<ReturnRequestItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    public void addItem(ReturnRequestItem item) {
        item.setReturnRequest(this);
        this.items.add(item);
    }
}
