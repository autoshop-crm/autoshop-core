--liquibase formatted sql

--changeset progko:vehicle-umapi-catalog-1
ALTER TABLE vehicle ADD COLUMN IF NOT EXISTS umapi_type VARCHAR(20);
ALTER TABLE vehicle ADD COLUMN IF NOT EXISTS umapi_manufacturer_id INTEGER;
ALTER TABLE vehicle ADD COLUMN IF NOT EXISTS umapi_manufacturer_name VARCHAR(100);
ALTER TABLE vehicle ADD COLUMN IF NOT EXISTS umapi_model_series_id INTEGER;
ALTER TABLE vehicle ADD COLUMN IF NOT EXISTS umapi_model_series_name VARCHAR(150);
ALTER TABLE vehicle ADD COLUMN IF NOT EXISTS umapi_modification_id INTEGER;
ALTER TABLE vehicle ADD COLUMN IF NOT EXISTS umapi_modification_name VARCHAR(255);
ALTER TABLE vehicle ADD COLUMN IF NOT EXISTS umapi_engine_description VARCHAR(255);
ALTER TABLE vehicle ADD COLUMN IF NOT EXISTS umapi_catalog_linked_at TIMESTAMP;
--rollback ALTER TABLE vehicle DROP COLUMN IF EXISTS umapi_catalog_linked_at;
--rollback ALTER TABLE vehicle DROP COLUMN IF EXISTS umapi_engine_description;
--rollback ALTER TABLE vehicle DROP COLUMN IF EXISTS umapi_modification_name;
--rollback ALTER TABLE vehicle DROP COLUMN IF EXISTS umapi_modification_id;
--rollback ALTER TABLE vehicle DROP COLUMN IF EXISTS umapi_model_series_name;
--rollback ALTER TABLE vehicle DROP COLUMN IF EXISTS umapi_model_series_id;
--rollback ALTER TABLE vehicle DROP COLUMN IF EXISTS umapi_manufacturer_name;
--rollback ALTER TABLE vehicle DROP COLUMN IF EXISTS umapi_manufacturer_id;
--rollback ALTER TABLE vehicle DROP COLUMN IF EXISTS umapi_type;

--changeset progko:vehicle-umapi-catalog-2
CREATE INDEX IF NOT EXISTS ix_vehicle_umapi_modification_id
ON vehicle (umapi_modification_id);
--rollback DROP INDEX IF EXISTS ix_vehicle_umapi_modification_id;
