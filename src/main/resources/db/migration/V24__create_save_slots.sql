CREATE TABLE save_slots (
    id BIGSERIAL PRIMARY KEY,
    manager_id BIGINT NOT NULL REFERENCES managers(id),
    slot_name VARCHAR(100) NOT NULL,
    slot_number INTEGER NOT NULL,
    description VARCHAR(500),
    save_type VARCHAR(20) NOT NULL,
    current_tournament_id BIGINT REFERENCES tournaments(id),
    current_season INTEGER NOT NULL,
    current_stage VARCHAR(80) NOT NULL,
    total_play_time BIGINT NOT NULL,
    manager_level INTEGER NOT NULL,
    reputation VARCHAR(30) NOT NULL,
    tournaments_played INTEGER NOT NULL,
    trophies INTEGER NOT NULL,
    current_team VARCHAR(120) NOT NULL,
    current_tournament VARCHAR(120) NOT NULL,
    progress_percentage DOUBLE PRECISION NOT NULL,
    latest_save_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_played_at TIMESTAMP NOT NULL,
    autosave BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL,
    CONSTRAINT uk_save_slot_manager_slot_number
        UNIQUE (manager_id, slot_number)
);

CREATE INDEX idx_save_slots_manager
    ON save_slots(manager_id);

CREATE INDEX idx_save_slots_manager_active
    ON save_slots(manager_id, active);

CREATE INDEX idx_save_slots_manager_autosave
    ON save_slots(manager_id, autosave);

CREATE UNIQUE INDEX uk_save_slots_manager_autosave
    ON save_slots(manager_id)
    WHERE autosave = TRUE;
