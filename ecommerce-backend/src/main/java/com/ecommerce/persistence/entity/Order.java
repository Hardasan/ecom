package com.ecommerce.persistence.entity;

import com.ecommerce.persistence.entity.embeddable.AddressSnapshot;
import com.ecommerce.persistence.entity.enumeration.OrderStatus;
import com.ecommerce.persistence.entity.enumeration.PaymentMethod;
import com.ecommerce.persistence.entity.enumeration.ShippingZone;
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

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(name = "orders_seq", sequenceName = "orders_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 32)
    private PaymentMethod paymentMethod = PaymentMethod.ONLINE;

    @Embedded
    private AddressSnapshot shippingAddress = new AddressSnapshot();

    // -----------------------------------------------------------------------------------------
    // Money / shipping: totalCost = itemsCost - discountAmount + shippingCost
    // -----------------------------------------------------------------------------------------
    @Column(name = "items_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal itemsCost;

    @Column(name = "shipping_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal shippingCost;

    @Column(name = "total_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCost;

    // Applied discount code, snapshotted at checkout. discountId lets us release the redemption on
    // cancel/expiry; code + amount are frozen so the order total stays stable if the code changes.
    @Column(name = "discount_id")
    private Long discountId;

    @Column(name = "discount_code", length = 64)
    private String discountCode;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_weight_gram", nullable = false)
    private Integer totalWeightGram;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_zone", nullable = false, length = 32)
    private ShippingZone shippingZone;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 25)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 25)
    private List<Transaction> transactions = new ArrayList<>();

    @Column(name = "reserved_until")
    private Date reservedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Date createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Date updatedAt;

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void addTransaction(Transaction transaction) {
        transaction.setOrder(this);
        transactions.add(transaction);
    }
}
