
CREATE TABLE manager_economy (
    id BIGSERIAL PRIMARY KEY,
    manager_id BIGINT NOT NULL UNIQUE REFERENCES managers(id),
    balance INTEGER NOT NULL DEFAULT 0,
    training_allocation INTEGER NOT NULL DEFAULT 0,
    medical_allocation INTEGER NOT NULL DEFAULT 0,
    scouting_allocation INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE resource_transactions (
    id BIGSERIAL PRIMARY KEY,
    manager_id BIGINT NOT NULL REFERENCES managers(id),
    amount INTEGER NOT NULL,
    reason VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(100),
    transaction_date TIMESTAMP NOT NULL,
    CONSTRAINT uq_manager_idempotency UNIQUE(manager_id, idempotency_key)
);