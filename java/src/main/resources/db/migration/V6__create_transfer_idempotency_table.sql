CREATE TABLE transfer_idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    request_hash VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    transaction_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_idempotency_transaction_id FOREIGN KEY (transaction_id) REFERENCES transactions (id)
);

CREATE INDEX idx_transfer_idempotency_key ON transfer_idempotency_records (idempotency_key);
