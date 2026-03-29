-- =============================================
-- User Service: Contact Request System
-- Version: V2
-- Description: Redesigns contacts table for proper request/accept workflow
-- =============================================

-- -----------------------------------------
-- Step 1: Rename columns for clarity
-- -----------------------------------------

ALTER TABLE contacts
    RENAME COLUMN user_id TO requester_id;

ALTER TABLE contacts
    RENAME COLUMN contact_user_id TO recipient_id;

-- -----------------------------------------
-- Step 2: Add new columns
-- -----------------------------------------

ALTER TABLE contacts
    ADD COLUMN accepted_at TIMESTAMP NULL;

ALTER TABLE contacts
    ADD COLUMN blocked_by UUID NULL;

-- -----------------------------------------
-- Step 3: Drop old indexes
-- -----------------------------------------

DROP INDEX idx_contacts_user_id;
DROP INDEX idx_contacts_user_contact;

-- -----------------------------------------
-- Step 4: Add new constraints and indexes
-- -----------------------------------------

-- Prevent self-requests
ALTER TABLE contacts
    ADD CONSTRAINT chk_no_self_request
        CHECK (requester_id != recipient_id);

-- Unique constraint on sorted pair (prevents A→B and B→A)
CREATE UNIQUE INDEX idx_contact_unique_pair ON contacts (
    LEAST(requester_id, recipient_id),
    GREATEST(requester_id, recipient_id)
);

-- Incoming requests: "Who sent me requests?"
CREATE INDEX idx_contacts_recipient_status
    ON contacts(recipient_id, status);

-- Outgoing requests: "What requests did I send?"
CREATE INDEX idx_contacts_requester_status
    ON contacts(requester_id, status);

-- Blocked users: "Who did I block?"
CREATE INDEX idx_contacts_blocked_by
    ON contacts(blocked_by)
    WHERE status = 'BLOCKED';