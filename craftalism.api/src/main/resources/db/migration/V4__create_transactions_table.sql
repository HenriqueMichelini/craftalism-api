CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    amount BIGINT NOT NULL,
    from_player UUID,
    to_player UUID,
    created_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_from_player FOREIGN KEY (from_player) REFERENCES players (uuid),
    CONSTRAINT fk_to_player FOREIGN KEY (to_player) REFERENCES players (uuid),

    CHECK (amount > 0),
        CHECK (
            (from_player IS NOT NULL OR to_player IS NOT NULL)
        )
);

CREATE INDEX idx_tx_from ON transactions (from_player);
CREATE INDEX idx_tx_to ON transactions (to_player);
