CREATE TABLE players (
    uuid UUID PRIMARY KEY,
    name VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL
);