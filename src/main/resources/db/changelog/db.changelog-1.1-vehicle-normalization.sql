--liquibase formatted sql

--changeset progko:vehicle-normalization-1
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'car') AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'vehicle') THEN 1 ELSE 0 END
ALTER TABLE Car RENAME TO vehicle;
--rollback ALTER TABLE vehicle RENAME TO Car;

--changeset progko:vehicle-normalization-2
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'vehicle' AND column_name = 'licence_plat') AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'vehicle' AND column_name = 'license_plate') THEN 1 ELSE 0 END
ALTER TABLE vehicle RENAME COLUMN licence_plat TO license_plate;
--rollback ALTER TABLE vehicle RENAME COLUMN license_plate TO licence_plat;

--changeset progko:vehicle-normalization-3
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'customer_order' AND column_name = 'car_id') AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'customer_order' AND column_name = 'vehicle_id') THEN 1 ELSE 0 END
ALTER TABLE Customer_Order RENAME COLUMN car_id TO vehicle_id;
--rollback ALTER TABLE Customer_Order RENAME COLUMN vehicle_id TO car_id;

--changeset progko:vehicle-normalization-4
ALTER TABLE vehicle ALTER COLUMN vin SET NOT NULL;
ALTER TABLE vehicle ALTER COLUMN license_plate SET NOT NULL;
--rollback ALTER TABLE vehicle ALTER COLUMN vin DROP NOT NULL;
--rollback ALTER TABLE vehicle ALTER COLUMN license_plate DROP NOT NULL;

--changeset progko:vehicle-normalization-5
CREATE UNIQUE INDEX IF NOT EXISTS uk_vehicle_license_plate ON vehicle (license_plate);
--rollback DROP INDEX IF EXISTS uk_vehicle_license_plate;
