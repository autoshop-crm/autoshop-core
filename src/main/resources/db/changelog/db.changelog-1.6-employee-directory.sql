--liquibase formatted sql

--changeset progko:employee-email-column
ALTER TABLE Employee
    ADD COLUMN IF NOT EXISTS email VARCHAR(100);
--rollback ALTER TABLE Employee DROP COLUMN IF EXISTS email;

--changeset progko:employee-email-index
CREATE UNIQUE INDEX IF NOT EXISTS uk_employee_email ON Employee (email);
--rollback DROP INDEX IF EXISTS uk_employee_email;
