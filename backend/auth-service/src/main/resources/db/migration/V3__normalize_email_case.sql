-- =============================================
-- Auth Service: Normalize Email Case
-- Version: V3
-- Description: Normalizes existing email values to lowercase and
--              replaces the case-sensitive unique constraint with a
--              case-insensitive functional unique index on LOWER(email).
-- =============================================

-- Normalize all existing email values to lowercase
UPDATE users SET email = LOWER(email);

-- Drop the old case-sensitive unique constraint
ALTER TABLE users DROP CONSTRAINT uq_users_email;

-- Create a case-insensitive unique index using a functional expression
CREATE UNIQUE INDEX uq_users_email_lower ON users (LOWER(email));
