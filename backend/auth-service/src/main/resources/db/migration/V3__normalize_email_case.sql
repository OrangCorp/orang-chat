-- =============================================
-- Auth Service: Normalize Email Case
-- Version: V3
-- Description: Normalizes existing email values to lowercase and
--              replaces the case-sensitive unique constraint with a
--              case-insensitive functional unique index on LOWER(email).
-- =============================================

-- Remove duplicate accounts that share the same email (case-insensitive).
-- For each group of duplicates, retain the verified account (email_verified DESC),
-- or the oldest account if no verified account exists (created_at ASC).
DELETE FROM users
WHERE id IN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY LOWER(email)
                   ORDER BY email_verified DESC, created_at ASC, id ASC
               ) AS rn
        FROM users
    ) ranked
    WHERE rn > 1
);

-- Normalize all remaining email values to lowercase
UPDATE users SET email = LOWER(email);

-- Drop the old case-sensitive unique constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS uq_users_email;

-- Create a case-insensitive unique index using a functional expression
CREATE UNIQUE INDEX uq_users_email_lower ON users (LOWER(email));
