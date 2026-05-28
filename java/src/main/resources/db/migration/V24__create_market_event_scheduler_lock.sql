CREATE TABLE market_event_scheduler_locks (
    lock_name VARCHAR(64) PRIMARY KEY,
    owner VARCHAR(128) NOT NULL,
    lease_until TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO market_event_scheduler_locks (
    lock_name,
    owner,
    lease_until,
    updated_at
)
VALUES (
    'market_event_scheduler',
    'bootstrap',
    TIMESTAMP WITH TIME ZONE '1970-01-01T00:00:00Z',
    TIMESTAMP WITH TIME ZONE '1970-01-01T00:00:00Z'
);
