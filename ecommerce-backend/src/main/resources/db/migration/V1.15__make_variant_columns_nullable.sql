-- =============================================================================
-- V1.15 : Make variant columns nullable
-- Some products have no variant concept (single price). variantType and
-- variantValue are now optional at every level (product, price, cart_item,
-- order_item). The cart-line uniqueness constraint was tied to those columns
-- being NOT NULL; the line-merge semantics now live in CartService.
-- =============================================================================

ALTER TABLE PRODUCT
    ALTER COLUMN VARIANT_TYPE DROP NOT NULL;

ALTER TABLE PRODUCT_PRICE
    ALTER COLUMN VARIANT_VALUE DROP NOT NULL;

ALTER TABLE CART_ITEM
    ALTER COLUMN VARIANT_TYPE DROP NOT NULL,
    ALTER COLUMN VARIANT_VALUE DROP NOT NULL;

ALTER TABLE ORDER_ITEM
    ALTER COLUMN VARIANT_TYPE DROP NOT NULL,
    ALTER COLUMN VARIANT_VALUE DROP NOT NULL;

ALTER TABLE CART_ITEM DROP CONSTRAINT IF EXISTS UK_CART_ITEM_USER_PRODUCT_VARIANT;
