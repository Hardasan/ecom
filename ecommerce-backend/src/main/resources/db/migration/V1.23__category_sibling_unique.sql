-- =============================================================================
-- V1.23 : Sibling-scoped uniqueness for the 2-level category tree.
-- Root categories (parent_id IS NULL): name unique among roots.
-- Sub-categories: name unique among children of the same parent.
-- Postgres treats NULLs as distinct, so a single (parent_id, name) index would
-- not constrain roots — hence two partial indexes.
-- =============================================================================

CREATE UNIQUE INDEX UK_CATEGORY_ROOT_NAME
    ON CATEGORY (NAME)
    WHERE PARENT_ID IS NULL;

CREATE UNIQUE INDEX UK_CATEGORY_SUB_NAME
    ON CATEGORY (PARENT_ID, NAME)
    WHERE PARENT_ID IS NOT NULL;

CREATE INDEX IDX_CATEGORY_PARENT_ID ON CATEGORY (PARENT_ID);
