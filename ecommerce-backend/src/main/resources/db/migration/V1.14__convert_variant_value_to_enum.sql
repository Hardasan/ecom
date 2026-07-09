-- =============================================================================
-- V1.14 : Convert variant_value to typed enum
-- Price.variantValue, cart_item.variant_value, order_item.variant_value are now
-- backed by the VariantValue enum on the Java side. No schema change is
-- required: the column stays VARCHAR(64) and the existing values (e.g. 'RED',
-- 'BLUE', 'M', 'XL') are already valid enum names. This migration only records
-- the change in the Flyway history so subsequent version-bumps stay linear.
-- =============================================================================

SELECT 1;
