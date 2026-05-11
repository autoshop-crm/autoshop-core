--liquibase formatted sql

--changeset progko:order-requested-parts-1
CREATE TABLE IF NOT EXISTS order_requested_part
(
    id                      SERIAL PRIMARY KEY,
    order_id                INTEGER        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    article_number          VARCHAR(30)    NOT NULL,
    brand                   VARCHAR(20),
    name                    VARCHAR(100)   NOT NULL,
    umapi_article_id        INTEGER,
    matched_local_part_id   INTEGER REFERENCES part (id),
    requested_quantity      INTEGER        NOT NULL CHECK (requested_quantity > 0),
    status                  VARCHAR(32)    NOT NULL,
    selected_supplier       VARCHAR(50),
    selected_quote_signature VARCHAR(255),
    purchase_price          NUMERIC(10, 2),
    sale_price              NUMERIC(10, 2),
    currency                VARCHAR(10),
    delivery_days_min       INTEGER,
    delivery_days_max       INTEGER,
    quote_fetched_at        TIMESTAMP,
    ordered_at              TIMESTAMP,
    received_at             TIMESTAMP,
    created_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE order_requested_part;

--changeset progko:order-requested-parts-2
CREATE INDEX IF NOT EXISTS idx_order_requested_part_order_id ON order_requested_part (order_id);
--rollback DROP INDEX IF EXISTS idx_order_requested_part_order_id;
