-- =============================================================================
-- V1.29 : Return refund reference
-- When an admin pays back an APPROVED return (status -> REFUNDED), the bank
-- transfer reference is recorded here — the return-side twin of the reference on
-- order_transaction for the order-cancel refund flow. Nullable: it is only set on
-- the refund step and stays empty for REQUESTED/APPROVED/REJECTED returns.
-- =============================================================================
ALTER TABLE return_request
    ADD COLUMN REFUND_REFERENCE VARCHAR(255);
