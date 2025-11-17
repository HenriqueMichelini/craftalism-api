CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,
    amount BIGINT NOT NULL,
    from_player UUID,
    to_player UUID,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_from_player FOREIGN KEY (from_player) REFERENCES players (uuid),
    CONSTRAINT fk_to_player FOREIGN KEY (to_player) REFERENCES players (uuid)
);