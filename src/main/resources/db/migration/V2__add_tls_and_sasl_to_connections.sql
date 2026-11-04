-- TLS settings and the Kafka SASL mechanism of broker connections (ADR 0008). None of them is a
-- secret, so they are stored in plain text, next to the encrypted credentials.
--
-- sasl_mechanism:           Kafka connections with USERNAME_PASSWORD credentials only
-- tls_enabled:              Kafka only; Pulsar and RabbitMQ switch TLS on with their address scheme
-- tls_trusted_certificates: CA certificates in PEM form, trusted instead of the JVM's default CAs
ALTER TABLE broker_connection
    ADD COLUMN sasl_mechanism           VARCHAR(32),
    ADD COLUMN tls_enabled              BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN tls_trusted_certificates TEXT;
