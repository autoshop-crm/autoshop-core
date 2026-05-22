--liquibase formatted sql

--changeset progko:crm-approval-1
CREATE TABLE IF NOT EXISTS order_work_proposal
(
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    approval_type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    labor_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    parts_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    requires_every_extra_work_approval BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE order_work_proposal;

--changeset progko:crm-approval-2
CREATE TABLE IF NOT EXISTS order_approval_request
(
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    proposal_id INTEGER NOT NULL UNIQUE REFERENCES order_work_proposal (id) ON DELETE CASCADE,
    approval_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    request_token VARCHAR(120) NOT NULL UNIQUE,
    requested_amount NUMERIC(10, 2) NOT NULL,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    customer_contact_channel VARCHAR(50),
    decision_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE order_approval_request;

--changeset progko:crm-approval-3
CREATE TABLE IF NOT EXISTS order_approval_decision
(
    id SERIAL PRIMARY KEY,
    approval_request_id INTEGER NOT NULL REFERENCES order_approval_request (id) ON DELETE CASCADE,
    decision VARCHAR(32) NOT NULL,
    decision_token VARCHAR(120) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    comment VARCHAR(1000),
    decision_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE order_approval_decision;

--changeset progko:crm-approval-4
ALTER TABLE order_requested_part ADD COLUMN IF NOT EXISTS approval_request_id INTEGER REFERENCES order_approval_request (id);
--rollback ALTER TABLE order_requested_part DROP COLUMN IF EXISTS approval_request_id;
