-- =============================================================================
-- V1.9 : Product shipping weight
-- Weight (in grams) drives the postal shipping tariff at checkout. Existing rows
-- default to 0 so the column can be NOT NULL without back-filling each product.
-- =============================================================================

ALTER TABLE PRODUCT
    ADD COLUMN WEIGHT_GRAM INTEGER NOT NULL DEFAULT 0;
