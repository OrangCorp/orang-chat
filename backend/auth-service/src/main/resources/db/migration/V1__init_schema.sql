-- =============================================
-- Auth Service: Initial Schema
-- Version: V1
-- Description: Creates users table for authentication
-- =============================================

CREATE TABLE users (
                       id              UUID            PRIMARY KEY,
                       email           VARCHAR(255)    NOT NULL,
                       display_name    VARCHAR(255)    NOT NULL,
                       password_hash   VARCHAR(255)    NOT NULL,
                       created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                       CONSTRAINT uq_users_email UNIQUE (email)
);

-- =============================================
-- Notes:
-- • uq_users_email also serves as an index for login lookups
-- • TIMESTAMPTZ stores timezone info (important for distributed systems)
-- • DEFAULT NOW() ensures timestamps are auto-populated
-- =============================================