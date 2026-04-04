-- =============================================
-- Message Service: Rich Message Features
-- Version: V4
-- Description: Adds edit/delete, reactions, read pointers, and pinning
-- =============================================

-- -----------------------------------------
-- Step 1: Enhance messages table for edit/delete
-- -----------------------------------------

-- Soft delete: message stays in DB but marked as deleted
ALTER TABLE messages
    ADD COLUMN edited_at TIMESTAMP,
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by UUID;

-- Index for filtering out deleted messages efficiently
CREATE INDEX idx_messages_deleted_at ON messages(deleted_at)
    WHERE deleted_at IS NULL;

-- -----------------------------------------
-- Step 2: Create message_reactions table
-- One reaction per user per message (toggle/replace behavior)
-- -----------------------------------------

CREATE TABLE message_reactions (
                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   message_id UUID NOT NULL,
                                   user_id UUID NOT NULL,
                                   reaction_type VARCHAR(20) NOT NULL,
                                   created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Foreign key: when message hard-deleted, reactions go too
                                   CONSTRAINT fk_reactions_message
                                       FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,

    -- KEY CONSTRAINT: Only ONE reaction per user per message
    -- If user wants different reaction, they must replace
                                   CONSTRAINT uq_one_reaction_per_user_per_message
                                       UNIQUE(message_id, user_id),

    -- Validation: only allow predefined reaction types
    -- Added ORANG as your custom reaction! 🍊
                                   CONSTRAINT chk_reaction_type
                                       CHECK (reaction_type IN ('LIKE', 'HEART', 'LAUGH', 'WOW', 'SAD', 'ANGRY', 'ORANG'))
);

-- Index: get all reactions for a message (for aggregation)
CREATE INDEX idx_reactions_message ON message_reactions(message_id);

-- Index: get all reactions by a user (for "your reactions" feature)
CREATE INDEX idx_reactions_user ON message_reactions(user_id);

-- -----------------------------------------
-- Step 3: Replace old read receipts with read pointers
-- -----------------------------------------

-- Drop the old per-message table (less efficient pattern)
DROP TABLE IF EXISTS message_read_receipts CASCADE;

-- Create new read pointer table
-- One row per user per conversation = much more efficient!
CREATE TABLE read_receipts (
                               user_id UUID NOT NULL,
                               conversation_id UUID NOT NULL,
                               last_read_message_id UUID NOT NULL,
                               read_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Composite primary key: natural uniqueness
                               PRIMARY KEY (user_id, conversation_id),

    -- Foreign key to the "pointer" message
                               CONSTRAINT fk_read_receipt_message
                                   FOREIGN KEY (last_read_message_id) REFERENCES messages(id) ON DELETE CASCADE
);

-- Index for "get all read receipts for user" (conversation list with unread badges)
CREATE INDEX idx_read_receipts_user ON read_receipts(user_id);

-- Index for "get all receipts in a conversation" (if needed for analytics)
CREATE INDEX idx_read_receipts_conversation ON read_receipts(conversation_id);

-- -----------------------------------------
-- Step 4: Create pinned_messages table
-- -----------------------------------------

CREATE TABLE pinned_messages (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 conversation_id UUID NOT NULL,
                                 message_id UUID NOT NULL,
                                 pinned_by UUID NOT NULL,
                                 pinned_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Foreign keys with CASCADE for automatic cleanup
                                 CONSTRAINT fk_pinned_conversation
                                     FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
                                 CONSTRAINT fk_pinned_message
                                     FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,

    -- Prevent duplicate pins of same message
                                 CONSTRAINT uq_pinned_message_per_conversation
                                     UNIQUE(conversation_id, message_id)
);

-- Index for "get all pinned messages in conversation"
CREATE INDEX idx_pinned_conversation ON pinned_messages(conversation_id);

-- -----------------------------------------
-- Documentation
-- -----------------------------------------

COMMENT ON COLUMN messages.edited_at IS 'Timestamp when message was last edited. NULL if never edited.';
COMMENT ON COLUMN messages.deleted_at IS 'Soft delete timestamp. NULL means message is active.';
COMMENT ON COLUMN messages.deleted_by IS 'UUID of user who deleted (sender or admin).';

COMMENT ON TABLE message_reactions IS 'Emoji reactions. One reaction per user per message (toggle behavior).';
COMMENT ON COLUMN message_reactions.reaction_type IS 'Enum: LIKE, HEART, LAUGH, WOW, SAD, ANGRY, ORANG';

COMMENT ON TABLE read_receipts IS 'Read pointer pattern. Tracks last message read per user per conversation.';
COMMENT ON TABLE pinned_messages IS 'Pinned messages for quick access. Any participant can pin.';