CREATE TABLE broker_connection (
    id          UUID          PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    broker_type VARCHAR(32)   NOT NULL,
    address     VARCHAR(2048) NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,
    version     BIGINT        NOT NULL,
    CONSTRAINT broker_connection_name_unique UNIQUE (name)
);
