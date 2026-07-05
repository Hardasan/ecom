-- =============================================================================
-- V1.11 : Add reservation timeout to orders so inventory can be held for a
-- configurable window after checkout and auto-released on expiry.
-- =============================================================================

ALTER TABLE orders ADD COLUMN reserved_until TIMESTAMP;
