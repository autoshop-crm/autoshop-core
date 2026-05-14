--liquibase formatted sql

--changeset progko:crm-timeline-1
CREATE TABLE IF NOT EXISTS order_timeline_entry
(
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    event_type VARCHAR(64) NOT NULL,
    visibility VARCHAR(32) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    actor_id BIGINT,
    effective_status VARCHAR(40),
    summary VARCHAR(255) NOT NULL,
    details_json VARCHAR(2000),
    dedupe_key VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE order_timeline_entry;

--changeset progko:crm-timeline-2
CREATE UNIQUE INDEX IF NOT EXISTS uk_order_timeline_dedupe ON order_timeline_entry (order_id, dedupe_key);
CREATE INDEX IF NOT EXISTS idx_order_timeline_order_occurred ON order_timeline_entry (order_id, occurred_at);
--rollback DROP INDEX IF EXISTS uk_order_timeline_dedupe;
--rollback DROP INDEX IF EXISTS idx_order_timeline_order_occurred;
