--liquibase formatted sql

--changeset progko:1
CREATE TABLE IF NOT EXISTS Customer
(
    id           SERIAL PRIMARY KEY,
    first_name   VARCHAR(50)        NOT NULL,
    last_name    VARCHAR(50)        NOT NULL,
    email        VARCHAR(50) UNIQUE NOT NULL,
    phone_number VARCHAR(16)        NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
--rollback DROP TABLE Customer;


--changeset progko:2
CREATE TABLE IF NOT EXISTS Car
(
    id           SERIAL PRIMARY KEY,
    customer_id  INTEGER     NOT NULL REFERENCES Customer (id) ON DELETE CASCADE,
    brand        VARCHAR(25) NOT NULL,
    model        VARCHAR(25) NOT NULL,
    vin          VARCHAR(17) UNIQUE,
    licence_plat VARCHAR(15),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
--rollback DROP TABLE Car;

--changeset progko:3
CREATE TABLE IF NOT EXISTS Employee
(
    id         SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name  VARCHAR(50) NOT NULL,
    function   VARCHAR(20) NOT NULL, --Hibernate enum
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
--rollback DROP TABLE Employee;

--changeset progko:4
CREATE TABLE IF NOT EXISTS Customer_Order
(
    id              SERIAL PRIMARY KEY,
    customer_id     INTEGER     NOT NULL REFERENCES Customer (id),
    car_id          INTEGER     NOT NULL REFERENCES Car (id),
    employee_id     INTEGER REFERENCES Employee (id),
    problem         TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'NEW',
    costs_total     NUMERIC(10, 2)       DEFAULT 0,
    discount_amount NUMERIC(10, 2)       DEFAULT 0,
    final_amount    NUMERIC(10, 2)       DEFAULT 0,
    created_at      TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    update_at       TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    completed_at    TIMESTAMP
    );
--rollback DROP TABLE Customer_Order;

--changeset progko:5
CREATE TABLE IF NOT EXISTS Part
(
    id             SERIAL PRIMARY KEY,
    brand          VARCHAR(20),
    name           VARCHAR(50)    NOT NULL,
    article_number VARCHAR(30) UNIQUE,
    cost           NUMERIC(10, 2) NOT NULL,
    stock_quantity INTEGER        NOT NULL DEFAULT 0,
    created_at     TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP               DEFAULT CURRENT_TIMESTAMP
    );
--rollback DROP TABLE Part;

--changeset progko:6
CREATE TABLE Order_Items
(
    id            SERIAL PRIMARY KEY,
    order_id      INTEGER        NOT NULL REFERENCES Customer_Order (id) ON DELETE CASCADE,
    part_id       INTEGER        NOT NULL REFERENCES Part (id),
    quantity      INTEGER        NOT NULL CHECK (quantity > 0),
    price_at_sale NUMERIC(10, 2) NOT NULL
);
--rollback DROP TABLE Order_Items;

--changeset progko:7
CREATE TABLE Services
(
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(50)    NOT NULL,
    base_price NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE Services;

--changeset progko:8
CREATE TABLE Order_service
(
    id          SERIAL PRIMARY KEY,
    order_id    INTEGER        NOT NULL REFERENCES Customer_Order (id) ON DELETE CASCADE,
    employee_id INTEGER REFERENCES Employee (id),
    service_id  INTEGER        NOT NULL REFERENCES Services (id),
    price       NUMERIC(10, 2) NOT NULL
);
--rollback DROP TABLE Order_service;

--changeset progko:9
CREATE TABLE Loyalty_tiers
(
    TierID                     SERIAL PRIMARY KEY,
    name                       VARCHAR(30) UNIQUE NOT NULL, -- Hibernate enum
    entry_spent_money          NUMERIC(10, 2)     NOT NULL,
    discount_percent           INTEGER            NOT NULL,
    max_points_payment_percent INTEGER            NOT NULL
);
--rollback DROP TABLE Loyalty_tiers;

--changeset progko:10
CREATE TABLE Loyalty_accounts
(
    AccountID    SERIAL PRIMARY KEY,
    customer_id  INTEGER UNIQUE NOT NULL REFERENCES Customer (id) ON DELETE CASCADE,
    tier_id      INTEGER        NOT NULL REFERENCES Loyalty_tiers (TierID),
    balance      INTEGER        NOT NULL DEFAULT 0,
    total_spent  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total_scores INTEGER        NOT NULL DEFAULT 0,
    created_at   TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP               DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE Loyalty_accounts;

--changeset progko:11
CREATE TABLE Loyalty_transactions
(
    TransactionID    SERIAL PRIMARY KEY,
    account_id       INTEGER     NOT NULL REFERENCES Loyalty_accounts (AccountID) ON DELETE CASCADE,
    order_id         INTEGER REFERENCES Customer_Order (id),
    date_transaction TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    count_scores     INTEGER     NOT NULL,
    operation_type   VARCHAR(30) NOT NULL -- Hibernate enum
);
--rollback DROP TABLE Loyalty_transactions;

--changeset progko:12
CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_phone_number ON Customer (phone_number);
--rollback DROP INDEX IF EXISTS uk_customer_phone_number;
