-- =============================================================================
-- V1.10 : Orders
-- An order is placed at checkout from a user's cart. The shipping address and the
-- per-line product details / prices are SNAPSHOTTED onto the order so it stays
-- accurate even if the address or the catalog change afterwards.
-- ("order" is a reserved word in SQL, so the table is named "orders".)
-- =============================================================================

-- Hibernate allocationSize = 50, so the sequences must increment by 50.
CREATE SEQUENCE ORDERS_SEQ
    START WITH 1
    INCREMENT BY 50 NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE ORDERS
(
    ID                    BIGINT         NOT NULL DEFAULT NEXTVAL('orders_seq'),
    USER_ID               BIGINT         NOT NULL,
    STATUS                VARCHAR(32)    NOT NULL,

    -- Shipping address snapshot.
    RECIPIENT_FIRST_NAME  VARCHAR(255)   NOT NULL,
    RECIPIENT_LAST_NAME   VARCHAR(255)   NOT NULL,
    RECIPIENT_MOBILE      VARCHAR(20)    NOT NULL,
    RECIPIENT_NATIONAL_ID VARCHAR(20),
    PROVINCE              VARCHAR(64)    NOT NULL,
    CITY                  VARCHAR(255)   NOT NULL,
    POSTAL_CODE           VARCHAR(20)    NOT NULL,
    ADDRESS_LINE          TEXT           NOT NULL,
    PLAQUE                VARCHAR(32),
    UNIT                  VARCHAR(32),

    -- Money / shipping (total_cost = items_cost + shipping_cost).
    ITEMS_COST            NUMERIC(14, 2) NOT NULL,
    SHIPPING_COST         NUMERIC(14, 2) NOT NULL,
    TOTAL_COST            NUMERIC(14, 2) NOT NULL,
    TOTAL_WEIGHT_GRAM     INTEGER        NOT NULL,
    SHIPPING_ZONE         VARCHAR(32)    NOT NULL,

    CREATED_AT            TIMESTAMP      NOT NULL DEFAULT NOW(),
    UPDATED_AT            TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT PK_ORDERS PRIMARY KEY (ID),
    CONSTRAINT FK_ORDERS_USER FOREIGN KEY (USER_ID) REFERENCES APP_USER (ID) ON DELETE CASCADE
);

CREATE INDEX IDX_ORDERS_USER ON ORDERS (USER_ID);

CREATE SEQUENCE ORDER_ITEM_SEQ
    START WITH 1
    INCREMENT BY 50 NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE ORDER_ITEM
(
    ID             BIGINT         NOT NULL DEFAULT NEXTVAL('order_item_seq'),
    ORDER_ID       BIGINT         NOT NULL,
    PRODUCT_ID     BIGINT         NOT NULL,
    PRODUCT_NAME   VARCHAR(255)   NOT NULL,
    PRODUCT_CODE   VARCHAR(100)   NOT NULL,
    VARIANT_TYPE   VARCHAR(64)    NOT NULL,
    QUANTITY       INTEGER        NOT NULL,
    UNIT_PRICE     NUMERIC(12, 2) NOT NULL,
    DISCOUNT_PRICE NUMERIC(12, 2),
    LINE_TOTAL     NUMERIC(14, 2) NOT NULL,

    CONSTRAINT PK_ORDER_ITEM PRIMARY KEY (ID),
    CONSTRAINT FK_ORDER_ITEM_ORDER FOREIGN KEY (ORDER_ID) REFERENCES ORDERS (ID) ON DELETE CASCADE
);

CREATE INDEX IDX_ORDER_ITEM_ORDER ON ORDER_ITEM (ORDER_ID);
