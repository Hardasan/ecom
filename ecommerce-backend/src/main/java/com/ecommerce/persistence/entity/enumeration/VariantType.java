package com.ecommerce.persistence.entity.enumeration;

import lombok.Getter;

import java.util.Set;

/**
 * The variant dimension a product is offered in. Each {@link VariantType} has an associated set
 * of allowed {@link VariantValue}s — see {@link VariantValue#allFor(VariantType)}.
 */
@Getter
public enum VariantType {

    COLOR,
    SIZE;

    public Set<VariantValue> getAllowedValues() {
        return VariantValue.allFor(this);
    }
}