-- Payment method per order.
-- Existing rows and online orders default to ONLINE (pay via the gateway before shipping).
-- CASH_ON_DELIVERY orders are placed without online payment and never expire (reserved_until stays
-- null), so an admin can ship them straight from RESERVED and cash is collected on delivery.
ALTER TABLE orders ADD COLUMN payment_method VARCHAR(32) NOT NULL DEFAULT 'ONLINE';
