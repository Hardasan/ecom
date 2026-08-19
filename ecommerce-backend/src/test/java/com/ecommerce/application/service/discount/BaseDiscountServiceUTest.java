package com.ecommerce.application.service.discount;

import com.ecommerce.persistence.entity.Discount;
import com.ecommerce.persistence.entity.enumeration.DiscountScope;
import com.ecommerce.persistence.entity.enumeration.DiscountType;
import com.ecommerce.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
abstract class BaseDiscountServiceUTest {

    protected static final Long USER_ID = 7L;
    protected static final Long DISCOUNT_ID = 100L;

    @Mock
    protected DiscountRepository discountRepository;
    @Mock
    protected CartItemRepository cartItemRepository;
    @Mock
    protected ProductRepository productRepository;
    @Mock
    protected CategoryRepository categoryRepository;
    @Mock
    protected OrderRepository orderRepository;

    protected DiscountService service;

    @BeforeEach
    void baseSetUp() {
        // Real mapper + calculator (pure); only the repositories are mocked.
        service = new DiscountService(discountRepository, new DiscountMapperImpl(), new DiscountCalculator(),
                cartItemRepository, productRepository, categoryRepository, orderRepository);
    }

    protected Discount discount(Long id, String code, DiscountType type, BigDecimal value, DiscountScope scope) {
        Discount discount = new Discount();
        discount.setId(id);
        discount.setCode(code);
        discount.setType(type);
        discount.setValue(value);
        discount.setScope(scope);
        discount.setUsageCount(0);
        return discount;
    }

    /**
     * A percentage/ALL code with no limits, expiry or minimum — the "everything is fine" baseline.
     */
    protected Discount usableDiscount() {
        return discount(DISCOUNT_ID, "SAVE20", DiscountType.PERCENTAGE, BigDecimal.valueOf(20), DiscountScope.ALL);
    }
}
