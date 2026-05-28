ALTER TABLE market_quotes
    ADD COLUMN IF NOT EXISTS pricing_context_version INTEGER NOT NULL DEFAULT 1;

ALTER TABLE market_quotes
    ADD COLUMN IF NOT EXISTS pressure_position BIGINT NOT NULL DEFAULT 0;

ALTER TABLE market_quotes
    ADD COLUMN IF NOT EXISTS drift_revision BIGINT NULL;

ALTER TABLE market_quotes
    ADD COLUMN IF NOT EXISTS named_event_instance_id BIGINT NULL;

ALTER TABLE market_quotes
    ADD COLUMN IF NOT EXISTS event_effect_version INTEGER NULL;
