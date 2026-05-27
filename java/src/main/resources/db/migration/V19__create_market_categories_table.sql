CREATE TABLE market_categories (
    category_id VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(128) NOT NULL,
    display_order INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO market_categories (
    category_id,
    display_name,
    display_order,
    created_at,
    updated_at
)
SELECT
    category_id,
    MIN(category_display_name),
    CASE category_id
        WHEN 'farming' THEN 0
        WHEN 'animal_products' THEN 1
        WHEN 'mob_drops' THEN 2
        WHEN 'natural_blocks' THEN 3
        WHEN 'decorative_blocks' THEN 4
        WHEN 'forestry' THEN 5
        WHEN 'minerals' THEN 6
        ELSE 1000
    END,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM market_items
GROUP BY category_id;

ALTER TABLE market_items
    ADD CONSTRAINT fk_market_items_category
        FOREIGN KEY (category_id)
        REFERENCES market_categories (category_id);

ALTER TABLE market_items
    DROP COLUMN category_display_name;
