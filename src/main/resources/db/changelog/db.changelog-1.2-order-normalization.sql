--liquibase formatted sql

--changeset progko:order-normalization-1
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'customer_order') AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'orders') THEN 1 ELSE 0 END
ALTER TABLE Customer_Order RENAME TO orders;
--rollback ALTER TABLE orders RENAME TO Customer_Order;

--changeset progko:order-normalization-2
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'update_at') AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'updated_at') THEN 1 ELSE 0 END
ALTER TABLE orders RENAME COLUMN update_at TO updated_at;
--rollback ALTER TABLE orders RENAME COLUMN updated_at TO update_at;

--changeset progko:order-normalization-3
ALTER TABLE orders ALTER COLUMN employee_id DROP NOT NULL;
--rollback ALTER TABLE orders ALTER COLUMN employee_id SET NOT NULL;

--changeset progko:order-normalization-4
ALTER TABLE orders ALTER COLUMN problem SET NOT NULL;
ALTER TABLE orders ALTER COLUMN status SET DEFAULT 'NEW';
--rollback ALTER TABLE orders ALTER COLUMN problem DROP NOT NULL;
--rollback ALTER TABLE orders ALTER COLUMN status DROP DEFAULT;
