-- =============================================================================
-- V1.13 : Move variant type to product
-- A product has exactly one variant type; product prices store only variant_value.
-- =============================================================================

ALTER TABLE PRODUCT
    ADD COLUMN VARIANT_TYPE VARCHAR(64);

UPDATE PRODUCT p
SET VARIANT_TYPE = pp.VARIANT_TYPE
FROM (
    SELECT PRODUCT_ID, MAX(VARIANT_TYPE) AS VARIANT_TYPE
    FROM PRODUCT_PRICE
    GROUP BY PRODUCT_ID
) pp
WHERE p.ID = pp.PRODUCT_ID;

ALTER TABLE PRODUCT
    ALTER COLUMN VARIANT_TYPE SET NOT NULL;

ALTER TABLE PRODUCT_PRICE
    DROP COLUMN VARIANT_TYPE;

