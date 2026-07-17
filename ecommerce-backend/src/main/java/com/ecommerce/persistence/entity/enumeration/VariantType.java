package com.ecommerce.persistence.entity.enumeration;

import lombok.Getter;

import java.util.Locale;
import java.util.Set;

@Getter
public enum VariantType {

    COLOR(Set.of("BLUE", "RED", "WHITE", "BLACK", "GRAY", "GREEN", "YELLOW", "ORANGE", "PURPLE", "PINK", "BROWN")),

    SIZE(Set.of("XS", "S", "M", "L", "XL", "XXL"));

    private final Set<String> allowedValues;

    VariantType(Set<String> allowedValues) {
        this.allowedValues = allowedValues;
    }

    public boolean isAllowedValue(String value) {
        if (value == null) {
            return false;
        }
        return allowedValues.contains(value.trim().toUpperCase(Locale.ROOT));
    }
}
