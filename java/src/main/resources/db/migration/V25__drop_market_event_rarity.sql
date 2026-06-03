ALTER TABLE market_event_instances
    DROP COLUMN IF EXISTS rarity;

ALTER TABLE market_event_templates
    DROP COLUMN IF EXISTS rarity;
