-- Collections of message templates and global environments (ADR 0006). Template texts are stored
-- as written, placeholders included. Headers and variables keep their order in sort_order.

CREATE TABLE collection (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    version    BIGINT       NOT NULL,
    CONSTRAINT collection_name_unique UNIQUE (name)
);

CREATE TABLE message_template (
    id               UUID         PRIMARY KEY,
    collection_id    UUID         NOT NULL REFERENCES collection (id) ON DELETE CASCADE,
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

CREATE TABLE message_template_header (
    template_id UUID    NOT NULL REFERENCES message_template (id) ON DELETE CASCADE,
    sort_order  INTEGER NOT NULL,
    name        TEXT    NOT NULL,
    value       TEXT    NOT NULL,
    PRIMARY KEY (template_id, sort_order)
);

CREATE TABLE environment (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    version    BIGINT       NOT NULL,
    CONSTRAINT environment_name_unique UNIQUE (name)
);

CREATE TABLE environment_variable (
    environment_id UUID         NOT NULL REFERENCES environment (id) ON DELETE CASCADE,
    sort_order     INTEGER      NOT NULL,
    name           TEXT         NOT NULL,
    value          TEXT         NOT NULL,
    PRIMARY KEY (environment_id, sort_order)
);
