ALTER TABLE market_quotes
    ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE market_quotes
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP WITH TIME ZONE NULL;

CREATE INDEX IF NOT EXISTS idx_market_quotes_status ON market_quotes (status);
