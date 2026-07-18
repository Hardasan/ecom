-- =============================================================================
-- V1.12 : Add variant value
-- Price selection and cart line identity are based on (variant_type, variant_value).
-- =============================================================================

ALTER TABLE PRODUCT_PRICE
    ADD COLUMN VARIANT_VALUE VARCHAR(64) NOT NULL;

ALTER TABLE CART_ITEM
    ADD COLUMN VARIANT_VALUE VARCHAR(64) NOT NULL;

ALTER TABLE CART_ITEM DROP CONSTRAINT IF EXISTS UK_CART_ITEM_USER_PRODUCT_VARIANT;
ALTER TABLE CART_ITEM
    ADD CONSTRAINT UK_CART_ITEM_USER_PRODUCT_VARIANT UNIQUE (USER_ID, PRODUCT_ID, VARIANT_TYPE, VARIANT_VALUE);

ALTER TABLE ORDER_ITEM
    ADD COLUMN VARIANT_VALUE VARCHAR(64) NOT NULL;

