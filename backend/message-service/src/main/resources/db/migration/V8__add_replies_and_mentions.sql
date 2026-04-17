-- =============================================
-- Message Service: Replies and Mentions
-- Version: V8
-- Description: Adds reply threading and @mention tracking
-- =============================================

ALTER TABLE messages
    ADD COLUMN reply_to_message_id UUID
        REFERENCES messages(id) ON DELETE SET NULL;

CREATE INDEX idx_messages_reply_to ON messages(reply_to_message_id);

CREATE TABLE message_mentions (
                                  id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                  message_id          UUID        NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
                                  conversation_id     UUID        NOT NULL,
                                  mentioned_user_id   UUID        NOT NULL,
                                  created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT uq_mention UNIQUE(message_id, mentioned_user_id)
);

-- For building message responses: "load mentions for this message"
CREATE INDEX idx_mentions_message      ON message_mentions(message_id);

-- Primary access pattern: "show me all messages where I was mentioned"
CREATE INDEX idx_mentions_user         ON message_mentions(mentioned_user_id);

-- Secondary access pattern: "mentions in this specific conversation"
CREATE INDEX idx_mentions_conversation ON message_mentions(conversation_id);

-- -----------------------------------------
-- Documentation
-- -----------------------------------------
COMMENT ON COLUMN messages.reply_to_message_id IS
    'References the message this is replying to. NULL if not a reply. SET NULL on delete.';

COMMENT ON TABLE message_mentions IS
    'Structured @mention records extracted from message content. One row per user per message.';

COMMENT ON COLUMN message_mentions.conversation_id IS
    'Denormalized from the parent message for query performance. Avoids join through messages table.';

COMMENT ON CONSTRAINT uq_mention ON message_mentions IS
    'A user can only be mentioned once per message, regardless of how many times @uuid appears in content.';