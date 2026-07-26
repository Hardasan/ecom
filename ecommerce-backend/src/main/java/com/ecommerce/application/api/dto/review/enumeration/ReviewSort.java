package com.ecommerce.application.api.dto.review.enumeration;

/**
 * Curated sort orders for the public review list. Exposed as a {@code sort} request param and
 * translated to a Spring Data {@code Sort} in the service, instead of accepting arbitrary sort input.
 */
public enum ReviewSort {

    NEWEST,
    OLDEST,
    HIGHEST,
    LOWEST
}
