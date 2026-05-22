--liquibase formatted sql

--changeset progko:parts-module-1
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'order_items') AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'order_part_item') THEN 1 ELSE 0 END
ALTER TABLE order_items RENAME TO order_part_item;
--rollback ALTER TABLE order_part_item RENAME TO order_items;

--changeset progko:parts-module-2
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'part' AND column_name = 'reserved_quantity'
ALTER TABLE part ADD COLUMN reserved_quantity INTEGER NOT NULL DEFAULT 0;
--rollback ALTER TABLE part DROP COLUMN reserved_quantity;

--changeset progko:parts-module-3
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'labor_total'
ALTER TABLE orders ADD COLUMN labor_total NUMERIC(10, 2) NOT NULL DEFAULT 0;
--rollback ALTER TABLE orders DROP COLUMN labor_total;

--changeset progko:parts-module-4
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'parts_total'
ALTER TABLE orders ADD COLUMN parts_total NUMERIC(10, 2) NOT NULL DEFAULT 0;
--rollback ALTER TABLE orders DROP COLUMN parts_total;

--changeset progko:parts-module-5
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_part_cost_non_negative'
ALTER TABLE part ADD CONSTRAINT chk_part_cost_non_negative CHECK (cost >= 0);
--rollback ALTER TABLE part DROP CONSTRAINT chk_part_cost_non_negative;

--changeset progko:parts-module-6
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_part_stock_non_negative'
ALTER TABLE part ADD CONSTRAINT chk_part_stock_non_negative CHECK (stock_quantity >= 0);
--rollback ALTER TABLE part DROP CONSTRAINT chk_part_stock_non_negative;

--changeset progko:parts-module-7
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_part_reserved_non_negative'
ALTER TABLE part ADD CONSTRAINT chk_part_reserved_non_negative CHECK (reserved_quantity >= 0);
--rollback ALTER TABLE part DROP CONSTRAINT chk_part_reserved_non_negative;

--changeset progko:parts-module-8
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_part_stock_gte_reserved'
ALTER TABLE part ADD CONSTRAINT chk_part_stock_gte_reserved CHECK (stock_quantity >= reserved_quantity);
--rollback ALTER TABLE part DROP CONSTRAINT chk_part_stock_gte_reserved;

--changeset progko:parts-module-9
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_order_part_item_order_part'
ALTER TABLE order_part_item ADD CONSTRAINT uk_order_part_item_order_part UNIQUE (order_id, part_id);
--rollback ALTER TABLE order_part_item DROP CONSTRAINT uk_order_part_item_order_part;

