-- =============================================
-- Message Service: Add Optimistic Locking
-- Version: V7
-- Description: Adds version column to attachments table
-- =============================================

ALTER TABLE attachments
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN attachments.version IS
    'Optimistic locking version.';
