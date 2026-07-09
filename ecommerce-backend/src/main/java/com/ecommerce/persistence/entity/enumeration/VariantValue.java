package com.ecommerce.persistence.entity.enumeration;

import lombok.Getter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Flat enum combining every allowed variant value across all {@link VariantType}s.
 * The persistence layer stores this as its enum name (e.g. {@code RED}, {@code M}) in a
 * {@code VARCHAR(64)} column. The owning product's {@link VariantType} (the parent
 * {@code PRODUCT.VARIANT_TYPE} column) decides which subset of these values is valid — see
 * {@link #isAllowedFor(VariantType)} and {@link VariantValueResolver#parseNullable}.
 */
@Getter
public enum VariantValue {

    RED(VariantType.COLOR),
    BLUE(VariantType.COLOR),
    BLACK(VariantType.COLOR),
    WHITE(VariantType.COLOR),
    GREEN(VariantType.COLOR),
    YELLOW(VariantType.COLOR),
    ORANGE(VariantType.COLOR),
    PURPLE(VariantType.COLOR),
    PINK(VariantType.COLOR),
    BROWN(VariantType.COLOR),
    GRAY(VariantType.COLOR),

    XS(VariantType.SIZE),
    S(VariantType.SIZE),
    M(VariantType.SIZE),
    L(VariantType.SIZE),
    XL(VariantType.SIZE),
    XXL(VariantType.SIZE);

    private final VariantType variantType;

    VariantValue(VariantType variantType) {
        this.variantType = variantType;
    }

    public boolean isAllowedFor(VariantType type) {
        return this.variantType == type;
    }

    public static Set<VariantValue> allFor(VariantType type) {
        if (type == null) {
            return Collections.emptySet();
        }
        EnumSet<VariantValue> result = EnumSet.noneOf(VariantValue.class);
        for (VariantValue v : values()) {
            if (v.variantType == type) {
                result.add(v);
            }
        }
        return Collections.unmodifiableSet(result);
    }
}
