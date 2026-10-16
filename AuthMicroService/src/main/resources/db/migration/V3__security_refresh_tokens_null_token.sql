-- V3__security_refresh_tokens_null_token.sql
-- Relax NOT NULL constraint on token column so that new refresh tokens are exclusively stored as secure SHA-256 hashes
-- Existing tokens remain backward-compatible while new tokens store NULL in the plaintext column

ALTER TABLE refresh_tokens MODIFY COLUMN token VARCHAR(255) NULL;
