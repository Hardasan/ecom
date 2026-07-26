-- =============================================================================
-- V1.17 : Product reviews & ratings
-- One review per (user, product): a 1-5 star rating plus optional title/comment.
-- New reviews start PENDING and become public only once an admin approves them.
-- No aggregate table -- the average is derived on read from the PUBLISHED rows.
-- =============================================================================

-- Hibernate allocationSize = 50, so the sequence must increment by 50.
CREATE SEQUENCE PRODUCT_REVIEW_SEQ
    START WITH 1
    INCREMENT BY 50 NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE PRODUCT_REVIEW
(
    ID                BIGINT       NOT NULL DEFAULT NEXTVAL('product_review_seq'),
    PRODUCT_ID        BIGINT       NOT NULL,
    USER_ID           BIGINT       NOT NULL,
    RATING            SMALLINT     NOT NULL,
    TITLE             VARCHAR(255),
    COMMENT           TEXT,
    AUTHOR_NAME       VARCHAR(511) NOT NULL,
    STATUS            VARCHAR(32)  NOT NULL DEFAULT 'PENDING', -- PENDING | PUBLISHED | HIDDEN
    VERIFIED_PURCHASE BOOLEAN      NOT NULL DEFAULT FALSE,
    CREATED_AT        TIMESTAMP    NOT NULL DEFAULT NOW(),
    UPDATED_AT        TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT PK_PRODUCT_REVIEW PRIMARY KEY (ID),
    CONSTRAINT UK_PRODUCT_REVIEW_USER_PRODUCT UNIQUE (USER_ID, PRODUCT_ID),
    CONSTRAINT CK_PRODUCT_REVIEW_RATING CHECK (RATING BETWEEN 1 AND 5),
    CONSTRAINT FK_PRODUCT_REVIEW_PRODUCT FOREIGN KEY (PRODUCT_ID) REFERENCES PRODUCT (ID) ON DELETE CASCADE,
    CONSTRAINT FK_PRODUCT_REVIEW_USER FOREIGN KEY (USER_ID) REFERENCES APP_USER (ID) ON DELETE CASCADE
);

-- Public listing and the rating summary both filter by (product_id, status).
CREATE INDEX IDX_PRODUCT_REVIEW_PRODUCT_STATUS ON PRODUCT_REVIEW (PRODUCT_ID, STATUS);
CREATE INDEX IDX_PRODUCT_REVIEW_USER ON PRODUCT_REVIEW (USER_ID);
