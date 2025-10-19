-- Add discriminator column for single-table inheritance in users
ALTER TABLE users
  ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'RIDER';

-- Ensure existing rows have a sensible default
UPDATE users SET user_type = 'RIDER' WHERE user_type IS NULL;


