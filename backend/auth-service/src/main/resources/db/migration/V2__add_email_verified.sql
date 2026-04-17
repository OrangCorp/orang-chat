-- =============================================
-- Auth Service: Add Email Verification
-- Version: V2
-- Description: Adds email_verified flag to users table
-- =============================================

ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Existing users predate email verification.
-- Mark them as verified so they aren't locked out.
UPDATE users SET email_verified = TRUE;