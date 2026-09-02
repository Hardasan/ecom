package com.ecommerce.persistence.entity.enumeration;

/**
 * @author AmirHossein ZamanZade
 * @since 12/25/25
 */
public enum UserRole {

    ROLE_ADMIN,

    // Warehouse staff: approve paid orders, hand them to the courier, and drive fulfillment statuses.
    ROLE_WAREHOUSE,

    ROLE_APP_USER
}
