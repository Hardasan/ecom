package com.ecommerce.persistence.entity.enumeration;

public enum ReviewStatus {

    /** Awaiting admin approval: not shown publicly and not counted in the summary. The default on creation. */
    PENDING,

    /** Approved by an admin: visible to everyone and counted in the rating summary. */
    PUBLISHED,

    /** Rejected or removed by an admin: hidden from the public list and excluded from the summary. */
    HIDDEN
}
