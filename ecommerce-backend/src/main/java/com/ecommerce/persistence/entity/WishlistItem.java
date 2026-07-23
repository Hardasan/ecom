package com.ecommerce.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Entity
@Table(
        name = "wishlist_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_wishlist_item_user_product",
                columnNames = {"user_id", "product_id"})
)
@Getter
@Setter
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "wishlist_item_seq")
    @SequenceGenerator(name = "wishlist_item_seq", sequenceName = "wishlist_item_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    // A wishlist item is an immutable (user, product) bookmark; nothing about it ever changes
    // once created, so there is no quantity/variant/updated_at like the cart carries.
    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Date createdAt;
}
