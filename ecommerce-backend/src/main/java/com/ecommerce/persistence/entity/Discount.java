package com.ecommerce.persistence.entity;

import com.ecommerce.persistence.entity.enumeration.DiscountScope;
import com.ecommerce.persistence.entity.enumeration.DiscountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * An admin-created discount code a customer applies at checkout. The code carries its whole
 * configuration (type, value, cap, minimum, scope, expiry, usage limits); the money it takes off is
 * computed at checkout from the eligible cart lines and snapshotted onto the {@link Order}.
 *
 * <p>{@code usageCount} is the authoritative global-redemption counter, moved only by the atomic
 * conditional updates in {@code DiscountRepository} (never by Hibernate dirty-checking), so a
 * usage limit holds under concurrency the same way {@code decrementInventory} guards stock.
 */
@Entity
@Table(
        name = "discount",
        uniqueConstraints = @UniqueConstraint(name = "uk_discount_code", columnNames = "code")
)
@Getter
@Setter
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "discount_seq")
    @SequenceGenerator(name = "discount_seq", sequenceName = "discount_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * Normalised to trimmed upper-case on write; matched case-insensitively at apply time.
     */
    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private DiscountType type;

    /**
     * Percentage (1-100) when {@code type=PERCENTAGE}; a flat money amount when {@code FIXED_AMOUNT}.
     */
    @Column(name = "value", nullable = false, precision = 14, scale = 2)
    private BigDecimal value;

    /**
     * Caps a percentage discount (see requirement 2-6). Ignored for fixed-amount codes.
     */
    @Column(name = "max_discount_amount", precision = 14, scale = 2)
    private BigDecimal maxDiscountAmount;

    /**
     * Minimum eligible (in-scope) subtotal required before the code applies. Null = no minimum.
     */
    @Column(name = "minimum_cart_amount", precision = 14, scale = 2)
    private BigDecimal minimumCartAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 32)
    private DiscountScope scope;

    @ElementCollection
    @CollectionTable(name = "discount_product", joinColumns = @JoinColumn(name = "discount_id"))
    @Column(name = "product_id")
    @BatchSize(size = 25)
    private Set<Long> productIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "discount_category", joinColumns = @JoinColumn(name = "discount_id"))
    @Column(name = "category_id")
    @BatchSize(size = 25)
    private Set<Long> categoryIds = new HashSet<>();

    /**
     * Null = never expires.
     */
    @Column(name = "expires_at")
    private Date expiresAt;

    /**
     * Total redemptions allowed across all users. Null = unlimited.
     */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    /**
     * Redemptions allowed per user. Null = unlimited.
     */
    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Date createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Date updatedAt;
}
