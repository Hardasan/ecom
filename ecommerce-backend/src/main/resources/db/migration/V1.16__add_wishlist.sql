-- =============================================================================
-- V1.16 : Wishlist
-- A user's wishlist is simply the set of wishlist_item rows owned by that user;
-- there is no wishlist aggregate row (mirrors the cart's user-keyed model). Each
-- row is a (user, product) bookmark, unique per pair — a product is either on a
-- user's wishlist or it is not. Deleting the user or the product cascades the row.
-- =============================================================================

-- Hibernate allocationSize = 50, so the sequence must increment by 50.
CREATE SEQUENCE WISHLIST_ITEM_SEQ
    START WITH 1
    INCREMENT BY 50 NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE WISHLIST_ITEM
(
    ID         BIGINT    NOT NULL DEFAULT NEXTVAL('wishlist_item_seq'),
    USER_ID    BIGINT    NOT NULL,
    PRODUCT_ID BIGINT    NOT NULL,
    CREATED_AT TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT PK_WISHLIST_ITEM PRIMARY KEY (ID),
    CONSTRAINT UK_WISHLIST_ITEM_USER_PRODUCT UNIQUE (USER_ID, PRODUCT_ID),
    CONSTRAINT FK_WISHLIST_ITEM_USER FOREIGN KEY (USER_ID) REFERENCES APP_USER (ID) ON DELETE CASCADE,
    CONSTRAINT FK_WISHLIST_ITEM_PRODUCT FOREIGN KEY (PRODUCT_ID) REFERENCES PRODUCT (ID) ON DELETE CASCADE
);

CREATE INDEX IDX_WISHLIST_ITEM_USER ON WISHLIST_ITEM (USER_ID);
