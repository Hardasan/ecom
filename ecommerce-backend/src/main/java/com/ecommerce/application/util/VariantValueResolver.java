package com.ecommerce.application.util;

import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import com.ecommerce.persistence.entity.enumeration.VariantValue;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class VariantValueResolver {

    private VariantValueResolver() {
    }

    public static Set<VariantValue> allValuesFor(VariantType type) {
        return type == null ? Collections.emptySet() : VariantValue.allFor(type);
    }

    public static Set<String> allowedNames(VariantType type) {
        Set<String> names = new LinkedHashSet<>();
        for (VariantValue value : allValuesFor(type)) {
            names.add(value.name());
        }
        return Collections.unmodifiableSet(names);
    }

    public static VariantValue parse(VariantType type, String raw) {
        if (type == null || raw == null || raw.isBlank()) {
            throw new EcommerceException(ECOMErrorType.VALIDATION_ERROR);
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (VariantValue v : VariantValue.values()) {
            if (v.isAllowedFor(type) && v.name().equals(normalized)) {
                return v;
            }
        }
        throw new EcommerceException(ECOMErrorType.VALIDATION_ERROR);
    }

    /**
     * Lenient variant-value parser. Returns {@code null} for any of:
     * <ul>
     *   <li>{@code type == null} (a variant-less product / cart line)</li>
     *   <li>{@code raw == null}</li>
     *   <li>blank-only string (e.g. an empty UI checkbox)</li>
     * </ul>
     * For non-blank values with a present type, behaves like {@link #parse(VariantType, String)} —
     * still throws {@code VALIDATION_ERROR} on a typo or a value that doesn't match the type.
     */
    public static VariantValue parseNullable(VariantType type, String raw) {
        if (type == null) {
            return null;
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parse(type, raw);
    }
}