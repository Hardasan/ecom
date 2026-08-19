package com.ecommerce.application.service.discount;

import java.math.BigDecimal;

/**
 * One cart/order line reduced to just what the discount engine needs: the product and its
 * category/sub-category (for scope matching) and the money the line contributes.
 *
 * @param lineTotal the effective line cost already snapshotted (quantity × effective unit price)
 */
public record DiscountableLine(Long productId, Long categoryId, Long subCategoryId,
                               BigDecimal lineTotal, int quantity) {
}
