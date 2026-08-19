package com.ecommerce.persistence.entity.enumeration;

public enum DiscountScope {

    /**
     * Applies to the whole cart.
     */
    ALL,

    /**
     * Applies only to the listed products ({@code discount_product}).
     */
    PRODUCTS,

    /**
     * Applies only to products in the listed categories ({@code discount_category}), matched on category or sub-category.
     */
    CATEGORIES
}
