package com.ecommerce.persistence.entity.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * Product details captured on an order line at checkout so the order stays accurate even if the
 * catalog product is later renamed or removed.
 */
@Embeddable
@Getter
@Setter
public class ProductSnapshot {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "product_code", nullable = false, length = 100)
    private String productCode;
}
