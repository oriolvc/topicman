-- The whole schema, squashed before v0.1.0 (ADR 0007, ADR 0010). It runs on PostgreSQL and on the
-- embedded H2 database, so use SQL both accept: `./gradlew check` runs the tests on both.
-- Ids come from each table's sequence, taken by the repository before an insert.

CREATE SEQUENCE broker_connection_id_seq;
CREATE SEQUENCE collection_id_seq;
CREATE SEQUENCE message_template_id_seq;
CREATE SEQUENCE environment_id_seq;

-- Broker connections. Credentials are AES-GCM encrypted by the application with the connection id
-- as associated data (ADR 0005); the type stays readable so connections can be listed without
-- decrypting. No ciphertext for credentials type NONE.
--
-- TLS settings and the Kafka SASL mechanism are not secrets, so they are stored in plain text, next
-- to the encrypted credentials (ADR 0008):
-- sasl_mechanism:           Kafka connections with USERNAME_PASSWORD credentials only
-- tls_enabled:              Kafka only; Pulsar and RabbitMQ switch TLS on with their address scheme
-- tls_trusted_certificates: CA certificates in PEM form, trusted instead of the JVM's default CAs
CREATE TABLE broker_connection (
    id                       BIGINT                   PRIMARY KEY,
    name                     VARCHAR(100)             NOT NULL,
    broker_type              VARCHAR(32)              NOT NULL,
    address                  VARCHAR(2048)            NOT NULL,
    credentials_type         VARCHAR(32)              NOT NULL,
    credentials_ciphertext   BYTEA,
    sasl_mechanism           VARCHAR(32),
    tls_enabled              BOOLEAN                  NOT NULL DEFAULT FALSE,
    tls_trusted_certificates TEXT,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    version                  BIGINT                   NOT NULL,
    CONSTRAINT broker_connection_name_unique UNIQUE (name)
);

-- Collections of message templates and global environments (ADR 0006). Template texts are stored
-- as written, placeholders included. Headers and variables keep their order in sort_order.

CREATE TABLE collection (
    id         BIGINT                   PRIMARY KEY,
    name       VARCHAR(100)             NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version    BIGINT                   NOT NULL,
    CONSTRAINT collection_name_unique UNIQUE (name)
);

CREATE TABLE message_template (
    id               BIGINT       PRIMARY KEY,
    collection_id    BIGINT       NOT NULL REFERENCES collection (id) ON DELETE CASCADE,
    sort_order       INTEGER      NOT NULL,
    name             VARCHAR(100) NOT NULL,
    broker_type      VARCHAR(32)  NOT NULL,
    destination_kind VARCHAR(16)  NOT NULL,
    destination_name TEXT         NOT NULL,
    -- Set only for exchanges; may be empty for fanout exchanges.
    routing_key      TEXT,
    message_key      TEXT,
    payload_format   VARCHAR(16)  NOT NULL,
    payload_content  TEXT         NOT NULL,
    CONSTRAINT message_template_name_unique UNIQUE (collection_id, name)
);

-- entry_value, as `value` is a reserved word in H2.
CREATE TABLE message_template_header (
    template_id BIGINT  NOT NULL REFERENCES message_template (id) ON DELETE CASCADE,
    sort_order  INTEGER NOT NULL,
    name        TEXT    NOT NULL,
    entry_value TEXT    NOT NULL,
    PRIMARY KEY (template_id, sort_order)
);

CREATE TABLE environment (
    id         BIGINT                   PRIMARY KEY,
    name       VARCHAR(100)             NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version    BIGINT                   NOT NULL,
    CONSTRAINT environment_name_unique UNIQUE (name)
);

CREATE TABLE environment_variable (
    environment_id BIGINT  NOT NULL REFERENCES environment (id) ON DELETE CASCADE,
    sort_order     INTEGER NOT NULL,
    name           TEXT    NOT NULL,
    entry_value    TEXT    NOT NULL,
    PRIMARY KEY (environment_id, sort_order)
);
