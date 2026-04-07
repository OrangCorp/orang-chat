-- =============================================
-- Message Service: File Attachments
-- Version: V5
-- Description: Adds attachment storage with soft delete and cleanup support
-- =============================================

-- -----------------------------------------
-- Table: attachments
-- Stores metadata about uploaded files (actual files live in MinIO)
-- -----------------------------------------
CREATE TABLE attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    conversation_id UUID NOT NULL,
    uploader_id UUID NOT NULL,
    message_id UUID,

    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,

    -- Format: {conversationId}/{attachmentId}/{sanitized-filename}
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    thumbnail_storage_key VARCHAR(500),
    thumbnail_generated BOOLEAN DEFAULT FALSE,

    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    permanently_deleted_at TIMESTAMP,

    CONSTRAINT fk_attachments_conversation
                         FOREIGN KEY (conversation_id)
                         REFERENCES conversations(id)
                         ON DELETE CASCADE,

    CONSTRAINT fk_attachments_message
                         FOREIGN KEY (message_id)
                         REFERENCES messages(id)
                         ON DELETE SET NULL,

    CONSTRAINT chk_file_size
                         CHECK (file_size >= 0 AND file_size <= 52428800) -- 50MB max

);

-- -----------------------------------------
-- Indexes for performance
-- -----------------------------------------

CREATE INDEX idx_attachments_conversation ON attachments (conversation_id);
CREATE INDEX idx_attachments_message ON attachments (message_id);
CREATE INDEX idx_attachments_uploader ON attachments (uploader_id);
CREATE INDEX idx_attachments_deleted ON attachments (deleted_at) WHERE deleted_at IS NOT NULL;

-- "Find orphaned attachments" (uploaded but never attached to message)
CREATE INDEX idx_attachments_orphaned ON attachments (uploaded_at, message_id) WHERE message_id IS NOT NULL;

-- -----------------------------------------
-- Comments for documentation
-- -----------------------------------------

COMMENT ON TABLE attachments IS
    'File attachment metadata. Actual files stored in MinIO object storage.';

COMMENT ON COLUMN attachments.storage_key IS
    'Unique path in MinIO: {conversationId}/{attachmentId}/{filename}';

COMMENT ON COLUMN attachments.deleted_at IS
    'Soft delete timestamp. File remains in MinIO during 30-day grace period.';

COMMENT ON COLUMN attachments.permanently_deleted_at IS
    'Audit timestamp when file was physically deleted from MinIO by cleanup job.';

COMMENT ON INDEX idx_attachments_deleted IS
    'Partial index for cleanup job. Only indexes soft-deleted rows for efficiency.';