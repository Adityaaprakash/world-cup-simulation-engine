CREATE TABLE player_lifecycles (
    id BIGSERIAL PRIMARY KEY,
    manager_id BIGINT NOT NULL REFERENCES managers(id),
    player_id BIGINT NOT NULL REFERENCES players(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    retired BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(manager_id, player_id)
);
