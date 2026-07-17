package com.ecommerce.persistence.entity.enumeration;

import lombok.Getter;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Getter
public enum VariantType {

    COLOR(null),

    SIZE(Set.of("XS", "S", "M", "L", "XL", "XXL"));

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final Set<String> allowedValues;

    VariantType(Set<String> allowedValues) {
        this.allowedValues = allowedValues;
    }

    public boolean isAllowedValue(String value) {
        if (value == null) {
            return false;
        }
        if (this == COLOR) {
            return HEX_COLOR.matcher(value.trim()).matches();
        }
        return allowedValues.contains(value.trim().toUpperCase(Locale.ROOT));
    }
}
