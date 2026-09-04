-- =============================================================================
-- V1.28 : Customer returns (مرجوعی)
-- A shopper can request to return items from a delivered order within the return
-- window. A request holds the selected lines (with a per-line reason + refund
-- snapshot), the total refundable amount (Rial), and the شبا to pay it back to.
-- One request per order (uk_return_request_order); the money movement itself stays
-- on the existing admin order-refund flow.
-- Hibernate allocationSize = 50, so sequences increment by 50.
-- =============================================================================
CREATE SEQUENCE return_request_seq
    START WITH 1
    INCREMENT BY 50 NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE return_request_item_seq
    START WITH 1
    INCREMENT BY 50 NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE return_request
(
    ID            BIGINT         NOT NULL DEFAULT NEXTVAL('return_request_seq'),
    ORDER_ID      BIGINT         NOT NULL,
    USER_ID       BIGINT         NOT NULL,
    STATUS        VARCHAR(32)    NOT NULL,
    REFUND_AMOUNT NUMERIC(14, 2) NOT NULL DEFAULT 0,
    IBAN          VARCHAR(26),
    NOTE          VARCHAR(1000),
    CREATED_AT    TIMESTAMP      NOT NULL DEFAULT NOW(),
    UPDATED_AT    TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT PK_RETURN_REQUEST PRIMARY KEY (ID),
    CONSTRAINT UK_RETURN_REQUEST_ORDER UNIQUE (ORDER_ID),
    CONSTRAINT FK_RETURN_REQUEST_ORDER FOREIGN KEY (ORDER_ID) REFERENCES orders (ID),
    CONSTRAINT FK_RETURN_REQUEST_USER FOREIGN KEY (USER_ID) REFERENCES app_user (ID)
);

CREATE INDEX IDX_RETURN_REQUEST_USER ON return_request (USER_ID);

CREATE TABLE return_request_item
(
    ID                BIGINT         NOT NULL DEFAULT NEXTVAL('return_request_item_seq'),
    RETURN_REQUEST_ID BIGINT         NOT NULL,
    ORDER_ITEM_ID     BIGINT         NOT NULL,
    PRODUCT_NAME      VARCHAR(255)   NOT NULL,
    VARIANT_VALUE     VARCHAR(64),
    QUANTITY          INTEGER        NOT NULL,
    UNIT_PRICE        NUMERIC(12, 2) NOT NULL,
    LINE_REFUND       NUMERIC(14, 2) NOT NULL,
    REASON            VARCHAR(32)    NOT NULL,

    CONSTRAINT PK_RETURN_REQUEST_ITEM PRIMARY KEY (ID),
    CONSTRAINT FK_RETURN_ITEM_REQUEST FOREIGN KEY (RETURN_REQUEST_ID)
        REFERENCES return_request (ID) ON DELETE CASCADE
);

CREATE INDEX IDX_RETURN_ITEM_REQUEST ON return_request_item (RETURN_REQUEST_ID);
