--liquibase formatted sql

--changeset progko:crm-booking-1
CREATE TABLE IF NOT EXISTS service_category
(
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    display_order INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE service_category;

--changeset progko:crm-booking-2
ALTER TABLE services ADD COLUMN IF NOT EXISTS category_id INTEGER REFERENCES service_category (id);
ALTER TABLE services ADD COLUMN IF NOT EXISTS description VARCHAR(255);
ALTER TABLE services ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE services ADD COLUMN IF NOT EXISTS default_duration_minutes INTEGER;
--rollback ALTER TABLE services DROP COLUMN IF EXISTS category_id;
--rollback ALTER TABLE services DROP COLUMN IF EXISTS description;
--rollback ALTER TABLE services DROP COLUMN IF EXISTS active;
--rollback ALTER TABLE services DROP COLUMN IF EXISTS default_duration_minutes;

--changeset progko:crm-booking-3
CREATE TABLE IF NOT EXISTS service_inspection_item
(
    id SERIAL PRIMARY KEY,
    service_id INTEGER NOT NULL REFERENCES services (id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE service_inspection_item;

--changeset progko:crm-booking-4
ALTER TABLE orders ADD COLUMN IF NOT EXISTS planned_visit_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS planned_slot_minutes INTEGER;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS booking_channel VARCHAR(20);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS intake_notes TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS requires_owner_approval_for_every_extra_work BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS checked_in_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ready_for_owner_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS handed_over_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(40);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS planned_drop_off BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
--rollback ALTER TABLE orders DROP COLUMN IF EXISTS planned_visit_at;
--rollback ALTER TABLE orders DROP COLUMN IF EXISTS planned_slot_minutes;
--rollback ALTER TABLE orders DROP COLUMN IF EXISTS booking_channel;
--rollback ALTER TABLE orders DROP COLUMN IF EXISTS intake_notes;
--rollback ALTER TABLE orders DROP COLUMN IF EXISTS requires_owner_approval_for_every_extra_work;
--rollback ALTER TABLE orders DROP COLUMN IF EXISTS checked_in_at;
--rollback ALTER TABLE orders DROP COLUMN IF EXISTS ready_for_owner_at;
--rollback ALTER TABLE orders DROP COLUMN IF EXISTS handed_over_at;
--rollback ALTER TABLE orders DROP COLUMN IF EXISTS cancelled_at;
--rollback ALTER TABLE orders DROP COLUMN IF EXISTS cancellation_reason;
--rollback ALTER TABLE orders DROP COLUMN IF EXISTS planned_drop_off;
--rollback ALTER TABLE orders DROP COLUMN IF EXISTS version;

--changeset progko:crm-booking-5
CREATE INDEX IF NOT EXISTS idx_orders_planned_visit_at ON orders (planned_visit_at);
CREATE INDEX IF NOT EXISTS idx_orders_status_planned_visit_at ON orders (status, planned_visit_at);
--rollback DROP INDEX IF EXISTS idx_orders_planned_visit_at;
--rollback DROP INDEX IF EXISTS idx_orders_status_planned_visit_at;
