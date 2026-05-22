--liquibase formatted sql

--changeset progko:customer-auth-linkage-1
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'customer' AND column_name = 'auth_user_id'
ALTER TABLE customer ADD COLUMN auth_user_id BIGINT;
--rollback ALTER TABLE customer DROP COLUMN auth_user_id;

--changeset progko:customer-auth-linkage-2
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'customer' AND column_name = 'email_verified'
ALTER TABLE customer ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
--rollback ALTER TABLE customer DROP COLUMN email_verified;

--changeset progko:customer-auth-linkage-3
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'uk_customer_auth_user_id'
CREATE UNIQUE INDEX uk_customer_auth_user_id ON customer (auth_user_id) WHERE auth_user_id IS NOT NULL;
--rollback DROP INDEX IF EXISTS uk_customer_auth_user_id;
