package com.ecommerce.application.service.discount;

import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.Discount;
import com.ecommerce.persistence.entity.enumeration.DiscountScope;
import com.ecommerce.persistence.entity.enumeration.DiscountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiscountCalculator_computeUTest {

    private static final Long CAT = 5L;
    private static final Long SUB_CAT = 9L;

    private final DiscountCalculator calculator = new DiscountCalculator();

    // ---------------------------------------------------------------------------------------------
    // Percentage
    // ---------------------------------------------------------------------------------------------

    private static Discount percentage(BigDecimal value, BigDecimal maxDiscountAmount) {
        Discount discount = base(DiscountType.PERCENTAGE, value);
        discount.setMaxDiscountAmount(maxDiscountAmount);
        return discount;
    }

    private static Discount fixed(BigDecimal value) {
        return base(DiscountType.FIXED_AMOUNT, value);
    }

    private static Discount base(DiscountType type, BigDecimal value) {
        Discount discount = new Discount();
        discount.setType(type);
        discount.setValue(value);
        discount.setScope(DiscountScope.ALL);
        return discount;
    }

    private static DiscountableLine line(Long productId, BigDecimal lineTotal) {
        return new DiscountableLine(productId, CAT, null, lineTotal, 1);
    }

    // ---------------------------------------------------------------------------------------------
    // Fixed amount
    // ---------------------------------------------------------------------------------------------

    private static DiscountableLine categoryLine(Long productId, Long categoryId, Long subCategoryId,
            BigDecimal lineTotal) {
        return new DiscountableLine(productId, categoryId, subCategoryId, lineTotal, 1);
    }

    private static void assertAmount(long expected, BigDecimal actual) {
        assertEquals(0, BigDecimal.valueOf(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }

    // ---------------------------------------------------------------------------------------------
    // Scope
    // ---------------------------------------------------------------------------------------------

    @Test
    void percentage_over_whole_cart() {
        Discount discount = percentage(BigDecimal.valueOf(20), null);
        DiscountComputation result = calculator.compute(discount, List.of(
                line(1L, BigDecimal.valueOf(600_000)),
                line(2L, BigDecimal.valueOf(400_000))));

        assertAmount(200_000, result.amount());              // 20% of 1,000,000
        assertAmount(1_000_000, result.eligibleSubtotal());
    }

    @Test
    void percentage_capped_at_max_discount() {
        // The requirement's example: a 1,000,000 product, 20% code capped at 100,000.
        Discount discount = percentage(BigDecimal.valueOf(20), BigDecimal.valueOf(100_000));
        DiscountComputation result = calculator.compute(discount, List.of(line(1L, BigDecimal.valueOf(1_000_000))));

        assertAmount(100_000, result.amount());              // 200,000 raw, capped to 100,000
    }

    @Test
    void percentage_below_cap_uses_computed_value() {
        Discount discount = percentage(BigDecimal.valueOf(10), BigDecimal.valueOf(100_000));
        DiscountComputation result = calculator.compute(discount, List.of(line(1L, BigDecimal.valueOf(500_000))));

        assertAmount(50_000, result.amount());               // 10% = 50,000 < cap 100,000
    }

    @Test
    void percentage_rounds_half_up_to_two_decimals() {
        Discount discount = percentage(new BigDecimal("33.33"), null);
        DiscountComputation result = calculator.compute(discount, List.of(line(1L, new BigDecimal("100.00"))));

        // 100 * 33.33 / 100 = 33.33
        assertEquals(new BigDecimal("33.33"), result.amount());
    }

    // ---------------------------------------------------------------------------------------------
    // Minimum purchase
    // ---------------------------------------------------------------------------------------------

    @Test
    void fixed_amount_subtracts_flat_value() {
        Discount discount = fixed(BigDecimal.valueOf(50_000));
        DiscountComputation result = calculator.compute(discount, List.of(line(1L, BigDecimal.valueOf(300_000))));

        assertAmount(50_000, result.amount());
    }

    @Test
    void fixed_amount_clamped_to_eligible_subtotal() {
        // A code worth more than the cart can never drive the total negative.
        Discount discount = fixed(BigDecimal.valueOf(80_000));
        DiscountComputation result = calculator.compute(discount, List.of(line(1L, BigDecimal.valueOf(30_000))));

        assertAmount(30_000, result.amount());
    }

    @Test
    void products_scope_discounts_only_eligible_lines() {
        Discount discount = percentage(BigDecimal.valueOf(50), null);
        discount.setScope(DiscountScope.PRODUCTS);
        discount.setProductIds(Set.of(1L));

        DiscountComputation result = calculator.compute(discount, List.of(
                line(1L, BigDecimal.valueOf(200_000)),   // eligible
                line(2L, BigDecimal.valueOf(500_000))));  // not eligible

        assertAmount(100_000, result.amount());              // 50% of the eligible 200,000 only
        assertAmount(200_000, result.eligibleSubtotal());
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    @Test
    void categories_scope_matches_category_or_subcategory() {
        Discount discount = fixed(BigDecimal.valueOf(10_000));
        discount.setScope(DiscountScope.CATEGORIES);
        discount.setCategoryIds(Set.of(CAT));

        DiscountComputation result = calculator.compute(discount, List.of(
                categoryLine(1L, CAT, null, BigDecimal.valueOf(100_000)),      // by category
                categoryLine(2L, 99L, CAT, BigDecimal.valueOf(100_000)),       // by sub-category
                categoryLine(3L, 88L, 77L, BigDecimal.valueOf(500_000))));     // no match

        assertAmount(10_000, result.amount());
        assertAmount(200_000, result.eligibleSubtotal());    // only the two matching lines
    }

    @Test
    void products_scope_with_no_matching_line_throws_not_applicable() {
        Discount discount = percentage(BigDecimal.valueOf(20), null);
        discount.setScope(DiscountScope.PRODUCTS);
        discount.setProductIds(Set.of(999L));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> calculator.compute(discount, List.of(line(1L, BigDecimal.valueOf(100_000)))));
        assertEquals(ECOMErrorType.DISCOUNT_NOT_APPLICABLE, ex.getEcomErrorType());
    }

    @Test
    void empty_cart_throws_not_applicable() {
        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> calculator.compute(percentage(BigDecimal.valueOf(20), null), List.of()));
        assertEquals(ECOMErrorType.DISCOUNT_NOT_APPLICABLE, ex.getEcomErrorType());
    }

    @Test
    void below_minimum_cart_amount_throws() {
        Discount discount = percentage(BigDecimal.valueOf(20), null);
        discount.setMinimumCartAmount(BigDecimal.valueOf(500_000));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> calculator.compute(discount, List.of(line(1L, BigDecimal.valueOf(499_999)))));
        assertEquals(ECOMErrorType.DISCOUNT_MINIMUM_NOT_MET, ex.getEcomErrorType());
    }

    @Test
    void exactly_at_minimum_cart_amount_is_allowed() {
        Discount discount = percentage(BigDecimal.valueOf(20), null);
        discount.setMinimumCartAmount(BigDecimal.valueOf(500_000));

        DiscountComputation result = calculator.compute(discount, List.of(line(1L, BigDecimal.valueOf(500_000))));
        assertAmount(100_000, result.amount());
    }

    @Test
    void minimum_is_checked_against_eligible_subtotal_not_whole_cart() {
        // Minimum 300,000 with a product-scoped code: the out-of-scope line does not count toward it.
        Discount discount = fixed(BigDecimal.valueOf(10_000));
        discount.setScope(DiscountScope.PRODUCTS);
        discount.setProductIds(Set.of(1L));
        discount.setMinimumCartAmount(BigDecimal.valueOf(300_000));

        EcommerceException ex = assertThrows(EcommerceException.class,
                () -> calculator.compute(discount, List.of(
                        line(1L, BigDecimal.valueOf(200_000)),    // eligible, below the 300k minimum
                        line(2L, BigDecimal.valueOf(900_000)))));  // ineligible, ignored
        assertEquals(ECOMErrorType.DISCOUNT_MINIMUM_NOT_MET, ex.getEcomErrorType());
    }
}
