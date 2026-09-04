package com.ecommerce.persistence.entity.enumeration;

/**
 * Why a shopper is returning an item (علت مرجوعی). Stored per return line; the storefront renders
 * the Persian label for each value, so the backend keeps only the stable enum.
 */
public enum ReturnReason {
    SIZE_OR_COLOR_MISMATCH,
    DEFECTIVE,
    NOT_AS_DESCRIBED,
    CHANGED_MIND,
    OTHER
}
