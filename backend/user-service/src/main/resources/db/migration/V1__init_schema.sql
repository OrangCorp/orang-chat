-- =============================================
-- User Service: Initial Schema
-- Version: V1
-- Description: Creates profiles and contacts tables
-- =============================================

-- -----------------------------------------
-- Table: profiles
-- Stores user profile information
-- -----------------------------------------
CREATE TABLE profiles (
                          user_id         UUID            PRIMARY KEY,
                          display_name    VARCHAR(50)     NOT NULL,
                          bio             VARCHAR(500),
                          avatar_url      VARCHAR(500),
                          last_seen       TIMESTAMPTZ,
                          created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                          updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- -----------------------------------------
-- Table: contacts
-- Stores user relationships (friends, blocked, pending)
-- -----------------------------------------
CREATE TABLE contacts (
                          id              UUID            PRIMARY KEY,
                          user_id         UUID            NOT NULL,
                          contact_user_id UUID            NOT NULL,
                          status          VARCHAR(20)     NOT NULL,
                          created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
                          updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

                          CONSTRAINT chk_contacts_status
                              CHECK (status IN ('PENDING', 'ACCEPTED', 'BLOCKED'))
);

-- -----------------------------------------
-- Indexes
-- -----------------------------------------

-- Fast lookup: "Get all contacts for a user"
CREATE INDEX idx_contacts_user_id ON contacts(user_id);

-- Fast lookup: "Check if user X has user Y as contact"
CREATE INDEX idx_contacts_user_contact ON contacts(user_id, contact_user_id);