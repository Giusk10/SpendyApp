-- V2__security_refresh_tokens_hardening.sql
-- Add token_hash, family_id, revoked, and replaced_by to refresh_tokens table

ALTER TABLE refresh_tokens ADD COLUMN token_hash VARCHAR(64) NULL;
ALTER TABLE refresh_tokens ADD COLUMN family_id VARCHAR(64) NULL;
ALTER TABLE refresh_tokens ADD COLUMN revoked BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE refresh_tokens ADD COLUMN replaced_by VARCHAR(64) NULL;

CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
