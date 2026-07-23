-- Rename legacy order statuses to match the order lifecycle vocabulary.
UPDATE orders SET status = 'RESERVED' WHERE status = 'PENDING';
UPDATE orders SET status = 'FAILED' WHERE status = 'EXPIRED';
