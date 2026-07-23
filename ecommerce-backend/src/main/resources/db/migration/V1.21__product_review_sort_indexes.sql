-- =============================================================================
-- V1.21 : Covering indexes for the review list sort paths
-- Version jumps from V1.17: V1.18 - V1.20 are reserved by the order-management
-- branch (parallel branches reserve migration versions).
-- =============================================================================

-- Subsumed by the two composite indexes below (same leading columns).
DROP INDEX IDX_PRODUCT_REVIEW_PRODUCT_STATUS;

-- NEWEST (forward scan) and OLDEST (backward scan).
CREATE INDEX IDX_PRODUCT_REVIEW_PRODUCT_STATUS_CREATED
    ON PRODUCT_REVIEW (PRODUCT_ID, STATUS, CREATED_AT DESC, ID DESC);

-- HIGHEST / LOWEST sorts, the rating filter, and the summary group-by.
CREATE INDEX IDX_PRODUCT_REVIEW_PRODUCT_STATUS_RATING
    ON PRODUCT_REVIEW (PRODUCT_ID, STATUS, RATING, CREATED_AT DESC, ID DESC);
