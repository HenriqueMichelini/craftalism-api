CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    amount BIGINT NOT NULL,
    from_player_uuid UUID,
    to_player_uuid UUID,
    created_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_from_player_uuid FOREIGN KEY (from_player_uuid) REFERENCES players (uuid),
    CONSTRAINT fk_to_player_uuid FOREIGN KEY (to_player_uuid) REFERENCES players (uuid),

    CHECK (amount > 0),
        CHECK (
            (from_player_uuid IS NOT NULL OR to_player_uuid IS NOT NULL)
        )
);

CREATE INDEX idx_tx_from ON transactions (from_player_uuid);
CREATE INDEX idx_tx_to ON transactions (to_player_uuid);
