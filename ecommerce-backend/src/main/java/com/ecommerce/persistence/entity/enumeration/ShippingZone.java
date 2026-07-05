package com.ecommerce.persistence.entity.enumeration;

/**
 * Postal shipping zone of the destination relative to the dispatch origin province.
 * Drives the tariff lookup in the shipping calculator.
 */
public enum ShippingZone {

    /** Destination is in the same province as the origin (ارسال درون استانی). */
    INTRA_PROVINCE,

    /** Destination is in a province adjacent to the origin (برون استانی همجوار). */
    ADJACENT_PROVINCE,

    /** Destination is in a non-adjacent province (برون استانی غیرهمجوار). */
    NON_ADJACENT_PROVINCE
}
