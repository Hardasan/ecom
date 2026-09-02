-- =============================================================================
-- V1.27 : Order fulfillment
-- Warehouse staff (ROLE_WAREHOUSE) approve a paid order, hand it to a courier and
-- drive its status. The shipment details and the per-step timestamps are recorded
-- on the order so the fulfillment history is auditable. All columns are nullable:
-- they fill in as the order advances PAID/COD-RESERVED -> PROCESSING -> SENDING -> RECEIVED.
-- (The PROCESSING status itself needs no DDL: orders.status is a free VARCHAR.)
-- =============================================================================
ALTER TABLE orders
    ADD COLUMN carrier              VARCHAR(64),
    ADD COLUMN tracking_number      VARCHAR(128),
    ADD COLUMN approved_at          TIMESTAMP,
    ADD COLUMN shipped_at           TIMESTAMP,
    ADD COLUMN delivered_at         TIMESTAMP,
    ADD COLUMN fulfilled_by_user_id BIGINT;

-- Keep the audit pointer valid if the staff account is ever removed.
ALTER TABLE orders
    ADD CONSTRAINT fk_orders_fulfilled_by FOREIGN KEY (fulfilled_by_user_id)
        REFERENCES app_user (id) ON DELETE SET NULL;
