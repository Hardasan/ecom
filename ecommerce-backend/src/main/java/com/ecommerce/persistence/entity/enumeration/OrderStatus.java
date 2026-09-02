package com.ecommerce.persistence.entity.enumeration;

public enum OrderStatus {

    RESERVED,
    PAID,
    // Approved by warehouse staff and being prepared for dispatch (PAID/COD-RESERVED -> PROCESSING).
    PROCESSING,
    FAILED,
    CANCEL_BY_ADMIN,
    CANCEL_BY_USER,
    SENDING,
    RECEIVED
}
