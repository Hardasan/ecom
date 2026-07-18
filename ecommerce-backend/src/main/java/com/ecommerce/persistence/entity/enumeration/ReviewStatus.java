package com.ecommerce.persistence.entity.enumeration;

public enum ReviewStatus {

    /** Visible to everyone and counted in the rating summary. The default on creation. */
    PUBLISHED,

    /** Soft-moderated by an admin: hidden from the public list and excluded from the summary. */
    HIDDEN
}
