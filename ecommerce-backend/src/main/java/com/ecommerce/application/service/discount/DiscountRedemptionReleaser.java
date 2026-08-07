package com.ecommerce.application.service.discount;

import com.ecommerce.persistence.entity.Order;
import com.ecommerce.persistence.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Frees the discount redemption an order was holding — the counterpart of {@code OrderInventoryRestorer}.
 * Called alongside inventory restore whenever an order leaves a redemption-consuming state (cancel or
 * reservation expiry). A no-op for orders without a code, or when the discount has since been deleted
 * (the guarded update simply matches no row).
 */
@Component
@RequiredArgsConstructor
public class DiscountRedemptionReleaser {

    private final DiscountRepository discountRepository;

    public void release(Order order) {
        if (order.getDiscountId() != null) {
            discountRepository.releaseRedemption(order.getDiscountId());
        }
    }
}
