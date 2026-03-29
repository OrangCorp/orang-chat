-- =============================================
-- Message Service: Full-Text Search
-- Version: V2
-- Description: Adds search_vector column and indexes for message search
-- =============================================

-- Step 1: Add search_vector column (generated automatically from content)
ALTER TABLE messages
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED;

-- Step 2: Create GIN index for fast full-text search
-- GIN (Generalized Inverted Index) is optimized for tsvector lookups
-- This makes "WHERE search_vector @@ tsquery" queries fast
CREATE INDEX idx_messages_search_vector ON messages USING GIN(search_vector);

-- Step 3: Create composite index for "messages around" queries
-- Supports efficient before/after timestamp lookups within a conversation
CREATE INDEX idx_messages_conversation_created_asc ON messages(conversation_id, created_at ASC);