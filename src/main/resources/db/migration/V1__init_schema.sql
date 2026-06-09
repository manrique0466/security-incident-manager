CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'ANALYST',
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE incidents (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title                VARCHAR(200) NOT NULL,
    description          TEXT,
    priority             VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    status               VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    reporter_id          UUID         NOT NULL REFERENCES users(id),
    assigned_analyst_id  UUID         REFERENCES users(id),
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    resolved_at          TIMESTAMP,
    deleted_at           TIMESTAMP
);

CREATE INDEX idx_incidents_reporter    ON incidents(reporter_id);
CREATE INDEX idx_incidents_analyst     ON incidents(assigned_analyst_id);
CREATE INDEX idx_incidents_status      ON incidents(status);
CREATE INDEX idx_incidents_deleted_at  ON incidents(deleted_at);
