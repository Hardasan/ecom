package com.ecommerce.persistence.entity.enumeration;

/**
 * Lifecycle of a customer return (مرجوعی) request.
 * <p>
 * The customer-facing flow only ever creates a {@link #REQUESTED} request. An admin then reviews it
 * ({@link #APPROVED}/{@link #REJECTED}). An approved return's goods are sent to the warehouse, which
 * inspects and accepts them ({@link #RECEIVED} — this is where the stock is put back), or rejects
 * them back to {@link #REJECTED}. Finally the admin transfers the money and marks it
 * {@link #REFUNDED}. So: REQUESTED → APPROVED → RECEIVED → REFUNDED, with REJECTED reachable from
 * REQUESTED (admin) or APPROVED (warehouse).
 */
public enum ReturnStatus {
    REQUESTED,
    APPROVED,
    RECEIVED,
    REJECTED,
    REFUNDED
}
