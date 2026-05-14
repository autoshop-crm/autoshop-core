--liquibase formatted sql

--changeset progko:crm-query-1
CREATE INDEX IF NOT EXISTS idx_orders_customer_status ON orders (customer_id, status, id);
CREATE INDEX IF NOT EXISTS idx_orders_vehicle_status ON orders (vehicle_id, status, id);
CREATE INDEX IF NOT EXISTS idx_orders_employee_status ON orders (employee_id, status, id);
--rollback DROP INDEX IF EXISTS idx_orders_customer_status;
--rollback DROP INDEX IF EXISTS idx_orders_vehicle_status;
--rollback DROP INDEX IF EXISTS idx_orders_employee_status;
