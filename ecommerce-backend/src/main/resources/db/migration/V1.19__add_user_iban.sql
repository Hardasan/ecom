-- User IBAN for refunds when a paid order is cancelled.
ALTER TABLE app_user
    ADD COLUMN iban VARCHAR(26);
