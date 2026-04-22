--liquibase formatted sql

--changeset progko:loyalty-module-1
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'loyalty_tiers') AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'loyalty_tier') THEN 1 ELSE 0 END
ALTER TABLE loyalty_tiers RENAME TO loyalty_tier;
--rollback ALTER TABLE loyalty_tier RENAME TO loyalty_tiers;

--changeset progko:loyalty-module-2
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'loyalty_accounts') AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'loyalty_account') THEN 1 ELSE 0 END
ALTER TABLE loyalty_accounts RENAME TO loyalty_account;
--rollback ALTER TABLE loyalty_account RENAME TO loyalty_accounts;

--changeset progko:loyalty-module-3
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'loyalty_transactions') AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'loyalty_transaction') THEN 1 ELSE 0 END
ALTER TABLE loyalty_transactions RENAME TO loyalty_transaction;
--rollback ALTER TABLE loyalty_transaction RENAME TO loyalty_transactions;

--changeset progko:loyalty-module-4
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'loyalty_tier' AND column_name = 'tierid') AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'loyalty_tier' AND column_name = 'id') THEN 1 ELSE 0 END
ALTER TABLE loyalty_tier RENAME COLUMN tierid TO id;
--rollback ALTER TABLE loyalty_tier RENAME COLUMN id TO tierid;

--changeset progko:loyalty-module-5
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'loyalty_account' AND column_name = 'accountid') AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'loyalty_account' AND column_name = 'id') THEN 1 ELSE 0 END
ALTER TABLE loyalty_account RENAME COLUMN accountid TO id;
--rollback ALTER TABLE loyalty_account RENAME COLUMN id TO accountid;

--changeset progko:loyalty-module-6
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'loyalty_transaction' AND column_name = 'transactionid') AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'loyalty_transaction' AND column_name = 'id') THEN 1 ELSE 0 END
ALTER TABLE loyalty_transaction RENAME COLUMN transactionid TO id;
--rollback ALTER TABLE loyalty_transaction RENAME COLUMN id TO transactionid;

--changeset progko:loyalty-module-7
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'loyalty_account' AND column_name = 'total_scores') AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'loyalty_account' AND column_name = 'total_earned_points') THEN 1 ELSE 0 END
ALTER TABLE loyalty_account RENAME COLUMN total_scores TO total_earned_points;
--rollback ALTER TABLE loyalty_account RENAME COLUMN total_earned_points TO total_scores;

--changeset progko:loyalty-module-8
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'loyalty_transaction' AND column_name = 'count_scores') AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'loyalty_transaction' AND column_name = 'points_amount') THEN 1 ELSE 0 END
ALTER TABLE loyalty_transaction RENAME COLUMN count_scores TO points_amount;
--rollback ALTER TABLE loyalty_transaction RENAME COLUMN points_amount TO count_scores;

--changeset progko:loyalty-module-9
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'loyalty_transaction' AND column_name = 'date_transaction') AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'loyalty_transaction' AND column_name = 'created_at') THEN 1 ELSE 0 END
ALTER TABLE loyalty_transaction RENAME COLUMN date_transaction TO created_at;
--rollback ALTER TABLE loyalty_transaction RENAME COLUMN created_at TO date_transaction;

--changeset progko:loyalty-module-10
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'loyalty_transaction' AND column_name = 'reason'
ALTER TABLE loyalty_transaction ADD COLUMN reason VARCHAR(40) NOT NULL DEFAULT 'POINTS_APPLIED';
--rollback ALTER TABLE loyalty_transaction DROP COLUMN reason;

--changeset progko:loyalty-module-11
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'manual_discount_amount'
ALTER TABLE orders ADD COLUMN manual_discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0;
--rollback ALTER TABLE orders DROP COLUMN manual_discount_amount;

--changeset progko:loyalty-module-12
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'points_discount_amount'
ALTER TABLE orders ADD COLUMN points_discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0;
--rollback ALTER TABLE orders DROP COLUMN points_discount_amount;

--changeset progko:loyalty-module-13
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'loyalty_points_spent'
ALTER TABLE orders ADD COLUMN loyalty_points_spent INTEGER NOT NULL DEFAULT 0;
--rollback ALTER TABLE orders DROP COLUMN loyalty_points_spent;

--changeset progko:loyalty-module-14
UPDATE orders SET manual_discount_amount = discount_amount WHERE manual_discount_amount = 0 AND discount_amount > 0;
--rollback UPDATE orders SET manual_discount_amount = 0;

--changeset progko:loyalty-module-15
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_loyalty_account_balance_non_negative'
ALTER TABLE loyalty_account ADD CONSTRAINT chk_loyalty_account_balance_non_negative CHECK (balance >= 0);
--rollback ALTER TABLE loyalty_account DROP CONSTRAINT chk_loyalty_account_balance_non_negative;

--changeset progko:loyalty-module-16
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_loyalty_account_total_spent_non_negative'
ALTER TABLE loyalty_account ADD CONSTRAINT chk_loyalty_account_total_spent_non_negative CHECK (total_spent >= 0);
--rollback ALTER TABLE loyalty_account DROP CONSTRAINT chk_loyalty_account_total_spent_non_negative;

--changeset progko:loyalty-module-17
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_loyalty_account_total_earned_points_non_negative'
ALTER TABLE loyalty_account ADD CONSTRAINT chk_loyalty_account_total_earned_points_non_negative CHECK (total_earned_points >= 0);
--rollback ALTER TABLE loyalty_account DROP CONSTRAINT chk_loyalty_account_total_earned_points_non_negative;

--changeset progko:loyalty-module-18
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_loyalty_transaction_points_non_negative'
ALTER TABLE loyalty_transaction ADD CONSTRAINT chk_loyalty_transaction_points_non_negative CHECK (points_amount >= 0);
--rollback ALTER TABLE loyalty_transaction DROP CONSTRAINT chk_loyalty_transaction_points_non_negative;

--changeset progko:loyalty-module-19
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_loyalty_tier_discount_percent_range'
ALTER TABLE loyalty_tier ADD CONSTRAINT chk_loyalty_tier_discount_percent_range CHECK (discount_percent BETWEEN 0 AND 100);
--rollback ALTER TABLE loyalty_tier DROP CONSTRAINT chk_loyalty_tier_discount_percent_range;

--changeset progko:loyalty-module-20
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_loyalty_tier_max_points_percent_range'
ALTER TABLE loyalty_tier ADD CONSTRAINT chk_loyalty_tier_max_points_percent_range CHECK (max_points_payment_percent BETWEEN 0 AND 100);
--rollback ALTER TABLE loyalty_tier DROP CONSTRAINT chk_loyalty_tier_max_points_percent_range;

--changeset progko:loyalty-module-21
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_order_loyalty_points_spent_non_negative'
ALTER TABLE orders ADD CONSTRAINT chk_order_loyalty_points_spent_non_negative CHECK (loyalty_points_spent >= 0);
--rollback ALTER TABLE orders DROP CONSTRAINT chk_order_loyalty_points_spent_non_negative;

--changeset progko:loyalty-module-22
CREATE INDEX IF NOT EXISTS idx_loyalty_account_tier_id ON loyalty_account (tier_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_transaction_account_id ON loyalty_transaction (account_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_transaction_order_id ON loyalty_transaction (order_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_transaction_operation_type ON loyalty_transaction (operation_type);
--rollback DROP INDEX IF EXISTS idx_loyalty_transaction_operation_type;
--rollback DROP INDEX IF EXISTS idx_loyalty_transaction_order_id;
--rollback DROP INDEX IF EXISTS idx_loyalty_transaction_account_id;
--rollback DROP INDEX IF EXISTS idx_loyalty_account_tier_id;

--changeset progko:loyalty-module-23
INSERT INTO loyalty_tier (name, entry_spent_money, discount_percent, max_points_payment_percent)
VALUES
    ('BRONZE', 0, 0, 10),
    ('SILVER', 10000, 3, 20),
    ('GOLD', 30000, 5, 30),
    ('PLATINUM', 70000, 7, 40)
ON CONFLICT (name) DO UPDATE SET
    entry_spent_money = EXCLUDED.entry_spent_money,
    discount_percent = EXCLUDED.discount_percent,
    max_points_payment_percent = EXCLUDED.max_points_payment_percent;
--rollback DELETE FROM loyalty_tier WHERE name IN ('BRONZE', 'SILVER', 'GOLD', 'PLATINUM');
