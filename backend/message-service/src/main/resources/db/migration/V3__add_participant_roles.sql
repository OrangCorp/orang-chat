-- =============================================
-- Message Service: Participant Roles
-- Version: V3
-- Description: Adds role, joinedAt, addedBy to participants
-- =============================================

-- Step 1: Add columns to conversation_participants
ALTER TABLE conversation_participants
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    ADD COLUMN joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ADD COLUMN added_by UUID;

-- Step 2: Add created_by to conversations
ALTER TABLE conversations
    ADD COLUMN created_by UUID;

-- Step 3: Add constraint for role values
ALTER TABLE conversation_participants
    ADD CONSTRAINT chk_participant_role CHECK (role IN ('MEMBER', 'ADMIN'));

-- Step 4: Index for finding all groups a user is admin of
CREATE INDEX idx_conv_participants_user_role ON conversation_participants(user_id, role);