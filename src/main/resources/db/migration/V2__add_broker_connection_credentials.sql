-- Credentials are AES-GCM encrypted by the application (ADR 0005); the type stays readable so
-- connections can be listed without decrypting. No ciphertext for credentials type NONE.
ALTER TABLE broker_connection
    ADD COLUMN credentials_type       VARCHAR(32) NOT NULL DEFAULT 'NONE',
    ADD COLUMN credentials_ciphertext BYTEA;

ALTER TABLE broker_connection ALTER COLUMN credentials_type DROP DEFAULT;
