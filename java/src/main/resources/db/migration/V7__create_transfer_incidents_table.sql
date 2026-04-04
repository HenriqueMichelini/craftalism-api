CREATE TABLE transfer_incidents (
    id BIGSERIAL PRIMARY KEY,
    incident_type VARCHAR(64) NOT NULL,
    from_player_uuid UUID NULL,
    to_player_uuid UUID NULL,
    idempotency_key VARCHAR(128) NULL,
    reason VARCHAR(1024) NOT NULL,
    metadata VARCHAR(4000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_transfer_incidents_created_at ON transfer_incidents (created_at DESC);
CREATE INDEX idx_transfer_incidents_idempotency_key ON transfer_incidents (idempotency_key);
