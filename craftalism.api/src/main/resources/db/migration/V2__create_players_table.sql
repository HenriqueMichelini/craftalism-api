CREATE TABLE players (
    uuid UUID PRIMARY KEY,
    name VARCHAR(16) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),

    CHECK (char_length(name) >= 3)
);