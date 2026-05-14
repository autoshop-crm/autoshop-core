--liquibase formatted sql

--changeset progko:orders-status-column-length-1
ALTER TABLE orders ALTER COLUMN status TYPE VARCHAR(40);
--rollback ALTER TABLE orders ALTER COLUMN status TYPE VARCHAR(20);

--changeset progko:orders-status-column-length-2
ALTER TABLE orders ALTER COLUMN cancellation_reason TYPE VARCHAR(40);
--rollback ALTER TABLE orders ALTER COLUMN cancellation_reason TYPE VARCHAR(20);
