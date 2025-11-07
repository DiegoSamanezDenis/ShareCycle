-- Add bill breakdown columns and pricing plan to ledger_entry table
-- This migration supports the new Bill domain model with separate cost components

ALTER TABLE ledger_entry
    ADD COLUMN pricing_plan VARCHAR(50) NULL AFTER status,
    ADD COLUMN bill_id BINARY(16) NULL AFTER pricing_plan,
    ADD COLUMN bill_computed_at DATETIME NULL AFTER bill_id,
    ADD COLUMN base_cost DECIMAL(10,2) NOT NULL DEFAULT 0.0 AFTER bill_computed_at,
    ADD COLUMN time_cost DECIMAL(10,2) NOT NULL DEFAULT 0.0 AFTER base_cost,
    ADD COLUMN ebike_surcharge DECIMAL(10,2) NOT NULL DEFAULT 0.0 AFTER time_cost,
    ADD COLUMN total_cost DECIMAL(10,2) NOT NULL DEFAULT 0.0 AFTER ebike_surcharge;

-- Add check constraint to ensure total_cost is non-negative
ALTER TABLE ledger_entry
    ADD CONSTRAINT ck_ledger_total_cost CHECK (total_cost >= 0),
    ADD CONSTRAINT ck_ledger_base_cost CHECK (base_cost >= 0),
    ADD CONSTRAINT ck_ledger_time_cost CHECK (time_cost >= 0),
    ADD CONSTRAINT ck_ledger_ebike_surcharge CHECK (ebike_surcharge >= 0);

-- Create index on pricing_plan for analytics queries
CREATE INDEX idx_ledger_pricing_plan ON ledger_entry (pricing_plan, timestamp DESC);

-- Create index on bill_id for quick bill lookups
CREATE INDEX idx_ledger_bill_id ON ledger_entry (bill_id);

-- Drop the old total_amount column as it's replaced by total_cost
ALTER TABLE ledger_entry
    DROP COLUMN total_amount,
    DROP CONSTRAINT ck_ledger_total_amount;
