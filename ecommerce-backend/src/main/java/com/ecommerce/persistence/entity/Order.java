package com.ecommerce.persistence.entity;

import com.ecommerce.persistence.entity.embeddable.AddressSnapshot;
import com.ecommerce.persistence.entity.enumeration.OrderStatus;
import com.ecommerce.persistence.entity.enumeration.ShippingZone;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
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

    @Embedded
    private AddressSnapshot shippingAddress = new AddressSnapshot();

    // -----------------------------------------------------------------------------------------
    // Money / shipping: totalCost = itemsCost + shippingCost
    // -----------------------------------------------------------------------------------------
    @Column(name = "items_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal itemsCost;

    @Column(name = "shipping_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal shippingCost;

    @Column(name = "total_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "total_weight_gram", nullable = false)
    private Integer totalWeightGram;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_zone", nullable = false, length = 32)
    private ShippingZone shippingZone;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 25)
    private List<OrderItem> items = new ArrayList<>();

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
}
