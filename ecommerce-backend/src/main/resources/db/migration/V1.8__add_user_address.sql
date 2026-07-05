-- =============================================================================
-- V1.8 : User addresses
-- A user (registered or guest) may keep several saved shipping addresses.
-- =============================================================================

-- Hibernate allocationSize = 50, so the sequence must increment by 50.
CREATE SEQUENCE USER_ADDRESS_SEQ
    START WITH 1
    INCREMENT BY 50 NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE USER_ADDRESS
(
    ID                    BIGINT       NOT NULL DEFAULT NEXTVAL('user_address_seq'),
    USER_ID               BIGINT       NOT NULL,
    TITLE                 VARCHAR(255),
    RECIPIENT_FIRST_NAME  VARCHAR(255) NOT NULL,
    RECIPIENT_LAST_NAME   VARCHAR(255) NOT NULL,
    RECIPIENT_MOBILE      VARCHAR(20)  NOT NULL,
    RECIPIENT_NATIONAL_ID VARCHAR(20),
    PROVINCE              VARCHAR(64)  NOT NULL,
    CITY                  VARCHAR(255) NOT NULL,
    POSTAL_CODE           VARCHAR(20)  NOT NULL,
    ADDRESS_LINE          TEXT         NOT NULL,
    PLAQUE                VARCHAR(32),
    UNIT                  VARCHAR(32),
    IS_DEFAULT            BOOLEAN      NOT NULL DEFAULT FALSE,
    CREATED_AT            TIMESTAMP    NOT NULL DEFAULT NOW(),
    UPDATED_AT            TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT PK_USER_ADDRESS PRIMARY KEY (ID),
    CONSTRAINT FK_USER_ADDRESS_USER FOREIGN KEY (USER_ID) REFERENCES APP_USER (ID) ON DELETE CASCADE
);

CREATE INDEX IDX_USER_ADDRESS_USER ON USER_ADDRESS (USER_ID);
