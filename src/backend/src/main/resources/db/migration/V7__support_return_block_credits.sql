-- Support courtesy credits when a rider cannot return to a full station

ALTER TABLE ledger_entry
    ADD COLUMN description VARCHAR(255) NULL AFTER pricing_plan;

-- Allow ledger entries with negative totals to represent account credits
ALTER TABLE ledger_entry
    DROP CHECK ck_ledger_total_cost;

ALTER TABLE ledger_entry
    ADD CONSTRAINT ck_ledger_total_cost_range CHECK (total_cost >= -1000.00);

