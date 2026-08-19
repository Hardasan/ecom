package com.ecommerce.application.service.discount;

import com.ecommerce.persistence.entity.Order;
import com.ecommerce.persistence.repository.DiscountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DiscountRedemptionReleaser_UTest {

    @Mock
    private DiscountRepository discountRepository;

    private DiscountRedemptionReleaser releaser;

    @BeforeEach
    void setUp() {
        releaser = new DiscountRedemptionReleaser(discountRepository);
    }

    @Test
    void releases_the_slot_when_order_carries_a_discount() {
        Order order = new Order();
        order.setDiscountId(100L);

        releaser.release(order);

        verify(discountRepository).releaseRedemption(100L);
    }

    @Test
    void no_op_when_order_has_no_discount() {
        releaser.release(new Order()); // discountId is null

        verify(discountRepository, never()).releaseRedemption(anyLong());
    }
}
