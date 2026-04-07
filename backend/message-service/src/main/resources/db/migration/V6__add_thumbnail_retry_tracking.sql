-- =============================================
-- Message Service: Thumbnail Retry Tracking
-- Version: V6
-- Description: Adds columns to track thumbnail generation attempts
-- =============================================

ALTER TABLE attachments
    ADD COLUMN thumbnail_attempts INTEGER DEFAULT 0,
    ADD COLUMN thumbnail_last_attempt TIMESTAMP,
    ADD COLUMN thumbnail_error VARCHAR(500);

-- Index for retry job: find failed thumbnails that need retry
CREATE INDEX idx_attachments_thumbnail_retry
    ON attachments (thumbnail_attempts, thumbnail_last_attempt)
    WHERE thumbnail_generated = FALSE
        AND thumbnail_attempts > 0
        AND thumbnail_attempts < 3;

COMMENT ON COLUMN attachments.thumbnail_attempts IS
    'Number of thumbnail generation attempts. Max 3 before giving up.';

COMMENT ON COLUMN attachments.thumbnail_last_attempt IS
    'Timestamp of last thumbnail generation attempt. Used for retry backoff.';

COMMENT ON COLUMN attachments.thumbnail_error IS
    'Last error message from failed thumbnail generation.';