CREATE TABLE balance (
    uuid UUID PRIMARY KEY,
    amount BIGINT NOT NULL,

    CONSTRAINT fk_uuid FOREIGN KEY (uuid) REFERENCES players (uuid)
);