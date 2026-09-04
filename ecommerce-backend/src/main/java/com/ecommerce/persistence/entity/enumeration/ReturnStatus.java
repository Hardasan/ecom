package com.ecommerce.persistence.entity.enumeration;

/**
 * Lifecycle of a customer return (مرجوعی) request.
 * <p>
 * The customer-facing flow only ever creates a {@link #REQUESTED} request; an admin later reviews it
 * ({@link #APPROVED}/{@link #REJECTED}) and, once the money is transferred back through the existing
 * order-refund flow, marks it {@link #REFUNDED}.
 */
public enum ReturnStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    REFUNDED
}
