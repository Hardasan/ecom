package com.ecommerce.persistence.entity.enumeration;

/**
 * How an order is paid.
 *
 * <ul>
 *   <li>{@code ONLINE} — paid up front through the payment gateway; the order reserves stock for a
 *       limited window and is auto-released if payment is not confirmed in time.</li>
 *   <li>{@code CASH_ON_DELIVERY} — placed without online payment and settled in cash on delivery;
 *       such orders never expire (their {@code reserved_until} stays null) and are shippable by an
 *       admin straight from RESERVED.</li>
 * </ul>
 */
public enum PaymentMethod {

    ONLINE,

    CASH_ON_DELIVERY
}
