CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE user_role AS ENUM ('ADMIN', 'ANALYST');
CREATE TYPE incident_priority AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');
CREATE TYPE incident_status AS ENUM ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED');

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    role          user_role    NOT NULL DEFAULT 'ANALYST',
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE incidents (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title                VARCHAR(200) NOT NULL,
    description          TEXT,
    priority             incident_priority NOT NULL DEFAULT 'MEDIUM',
    status               incident_status   NOT NULL DEFAULT 'OPEN',
    reporter_id          UUID NOT NULL REFERENCES users(id),
    assigned_analyst_id  UUID REFERENCES users(id),
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at          TIMESTAMP,
    deleted_at           TIMESTAMP
);

CREATE INDEX idx_incidents_reporter    ON incidents(reporter_id);
CREATE INDEX idx_incidents_analyst     ON incidents(assigned_analyst_id);
CREATE INDEX idx_incidents_status      ON incidents(status);
CREATE INDEX idx_incidents_deleted_at  ON incidents(deleted_at);
