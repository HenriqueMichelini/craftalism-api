ALTER TABLE market_items
    ADD COLUMN IF NOT EXISTS drift_multiplier_basis_points BIGINT NOT NULL DEFAULT 10000;

ALTER TABLE market_items
    ADD COLUMN IF NOT EXISTS drift_revision BIGINT NOT NULL DEFAULT 0;

ALTER TABLE market_items
    ADD COLUMN IF NOT EXISTS drift_evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();

ALTER TABLE market_items
    ADD CONSTRAINT chk_market_items_drift_multiplier_positive
        CHECK (drift_multiplier_basis_points > 0);

ALTER TABLE market_items
    ADD CONSTRAINT chk_market_items_drift_revision_non_negative
        CHECK (drift_revision >= 0);
