CREATE TABLE maintenance_history (
    id BIGSERIAL PRIMARY KEY,
    operation VARCHAR(80) NOT NULL,
    administrator VARCHAR(100) NOT NULL,
    duration_ms BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    details VARCHAR(1000),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_maintenance_history_created_at
    ON maintenance_history(created_at DESC);

CREATE INDEX idx_maintenance_history_operation
    ON maintenance_history(operation);
