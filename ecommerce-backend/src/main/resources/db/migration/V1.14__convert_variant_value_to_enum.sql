-- =============================================================================
-- V1.14 : Convert variant_value to String (was previously an enum)
-- variantValue is now a plain String validated by VariantType.isAllowedValue().
-- No schema change is required: the column stays VARCHAR(64). This migration
-- only records the change in the Flyway history so subsequent version-bumps
-- stay linear.
-- =============================================================================

SELECT 1;
