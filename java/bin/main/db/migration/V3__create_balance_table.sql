CREATE TABLE balances (
    uuid UUID PRIMARY KEY,
    amount BIGINT NOT NULL,

    CHECK (amount >= 0),

    CONSTRAINT fk_uuid FOREIGN KEY (uuid) REFERENCES players (uuid)
);
