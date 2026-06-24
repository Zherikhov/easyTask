ALTER TABLE users DROP CONSTRAINT users_status_check;
ALTER TABLE users ADD CONSTRAINT users_status_check
    CHECK (status IN ('ACTIVE', 'DISABLED', 'PENDING'));

ALTER TABLE users ADD COLUMN invite_token VARCHAR(36);
ALTER TABLE users ADD COLUMN invite_token_expires_at TIMESTAMPTZ;

CREATE UNIQUE INDEX uq_users_invite_token ON users (invite_token);
