-- =============================================
-- Message Service: Initial Schema
-- Version: V1
-- Description: Creates conversations, messages, and related tables
-- =============================================

-- -----------------------------------------
-- Table: conversations
-- Stores chat conversations (direct or group)
-- -----------------------------------------
CREATE TABLE conversations (
                               id              UUID            PRIMARY KEY,
                               name            VARCHAR(255),
                               type            VARCHAR(20)     NOT NULL,
                               created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                               updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                               CONSTRAINT chk_conversations_type
                                   CHECK (type IN ('DIRECT', 'GROUP'))
);

-- -----------------------------------------
-- Table: conversation_participants
-- Junction table: which users are in which conversations
-- -----------------------------------------
CREATE TABLE conversation_participants (
                                           conversation_id UUID            NOT NULL,
                                           user_id         UUID            NOT NULL,

                                           CONSTRAINT pk_conversation_participants
                                               PRIMARY KEY (conversation_id, user_id),
                                           CONSTRAINT fk_conv_participants_conversation
                                               FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

-- -----------------------------------------
-- Table: messages
-- Stores individual chat messages
-- -----------------------------------------
CREATE TABLE messages (
                          id              UUID            PRIMARY KEY,
                          conversation_id UUID            NOT NULL,
                          sender_id       UUID            NOT NULL,
                          content         VARCHAR(2000)   NOT NULL,
                          created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                          updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                          CONSTRAINT fk_messages_conversation
                              FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

-- -----------------------------------------
-- Table: message_read_receipts
-- Tracks who read which messages and when
-- -----------------------------------------
CREATE TABLE message_read_receipts (
                                       message_id      UUID            NOT NULL,
                                       read_by         UUID            NOT NULL,
                                       read_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                       CONSTRAINT pk_message_read_receipts
                                           PRIMARY KEY (message_id, read_by),
                                       CONSTRAINT fk_read_receipts_message
                                           FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
);

-- -----------------------------------------
-- Indexes
-- -----------------------------------------

-- Fast lookup: "Get all messages in a conversation"
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);

-- Fast lookup: "Get messages in order" (for pagination)
CREATE INDEX idx_messages_conversation_created ON messages(conversation_id, created_at DESC);

-- Fast lookup: "Get all conversations for a user"
CREATE INDEX idx_conv_participants_user_id ON conversation_participants(user_id);