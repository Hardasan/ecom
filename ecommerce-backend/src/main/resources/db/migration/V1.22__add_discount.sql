-- =============================================================================
-- V1.22 : Discount codes
-- Admin-created codes applied at checkout. Value is a percentage (optionally capped
-- by max_discount_amount) or a flat amount; it may require a minimum eligible subtotal,
-- expire, and be limited globally (usage_limit / usage_count) and per user (per_user_limit).
-- Scope ALL | PRODUCTS | CATEGORIES: PRODUCTS/CATEGORIES targets live in the two child tables.
-- The money a code takes off is snapshotted onto the order (see orders.discount_* below).
-- =============================================================================

-- Hibernate allocationSize = 50, so the sequence must increment by 50.
CREATE SEQUENCE DISCOUNT_SEQ
    START WITH 1
    INCREMENT BY 50 NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE DISCOUNT
(
    ID                  BIGINT         NOT NULL DEFAULT NEXTVAL('discount_seq'),
    CODE                VARCHAR(64)    NOT NULL,
    TYPE                VARCHAR(32)    NOT NULL, -- PERCENTAGE | FIXED_AMOUNT
    VALUE               NUMERIC(14, 2) NOT NULL,
    MAX_DISCOUNT_AMOUNT NUMERIC(14, 2),          -- caps a percentage discount
    MINIMUM_CART_AMOUNT NUMERIC(14, 2),          -- min eligible subtotal to qualify
    SCOPE               VARCHAR(32)    NOT NULL, -- ALL | PRODUCTS | CATEGORIES
    EXPIRES_AT          TIMESTAMP,               -- null = never expires
    USAGE_LIMIT         INTEGER,                 -- null = unlimited (global)
    USAGE_COUNT         INTEGER        NOT NULL DEFAULT 0,
    PER_USER_LIMIT      INTEGER,                 -- null = unlimited (per user)
    CREATED_AT          TIMESTAMP      NOT NULL DEFAULT NOW(),
    UPDATED_AT          TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT PK_DISCOUNT PRIMARY KEY (ID),
    CONSTRAINT UK_DISCOUNT_CODE UNIQUE (CODE),
    CONSTRAINT CK_DISCOUNT_VALUE_POSITIVE CHECK (VALUE > 0),
    CONSTRAINT CK_DISCOUNT_USAGE_COUNT_NONNEG CHECK (USAGE_COUNT >= 0)
);

-- Product / category targets for scoped codes (empty for scope = ALL).
CREATE TABLE DISCOUNT_PRODUCT
(
    DISCOUNT_ID BIGINT NOT NULL,
    PRODUCT_ID  BIGINT NOT NULL,

    CONSTRAINT PK_DISCOUNT_PRODUCT PRIMARY KEY (DISCOUNT_ID, PRODUCT_ID),
    CONSTRAINT FK_DISCOUNT_PRODUCT_DISCOUNT FOREIGN KEY (DISCOUNT_ID) REFERENCES DISCOUNT (ID) ON DELETE CASCADE
);

CREATE TABLE DISCOUNT_CATEGORY
(
    DISCOUNT_ID BIGINT NOT NULL,
    CATEGORY_ID BIGINT NOT NULL,

    CONSTRAINT PK_DISCOUNT_CATEGORY PRIMARY KEY (DISCOUNT_ID, CATEGORY_ID),
    CONSTRAINT FK_DISCOUNT_CATEGORY_DISCOUNT FOREIGN KEY (DISCOUNT_ID) REFERENCES DISCOUNT (ID) ON DELETE CASCADE
);

-- Snapshot of the applied code on the order: id (to release the redemption on cancel/expiry),
-- code + amount (frozen so receipts/refunds stay stable even if the code is later edited/deleted).
ALTER TABLE ORDERS
    ADD COLUMN DISCOUNT_ID BIGINT,
    ADD COLUMN DISCOUNT_CODE   VARCHAR(64),
    ADD COLUMN DISCOUNT_AMOUNT NUMERIC(14, 2) NOT NULL DEFAULT 0;

ALTER TABLE ORDERS
    ADD CONSTRAINT FK_ORDERS_DISCOUNT FOREIGN KEY (DISCOUNT_ID) REFERENCES DISCOUNT (ID) ON DELETE SET NULL;

-- Per-user redemption count and redemption release both filter orders by discount_id.
CREATE INDEX IDX_ORDERS_DISCOUNT ON ORDERS (DISCOUNT_ID);
