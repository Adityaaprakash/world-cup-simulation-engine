ALTER TABLE save_slots
    ADD COLUMN format_version VARCHAR(20) NOT NULL DEFAULT '9I-2',
    ADD COLUMN manager_experience_points INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN backup_available BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN backup_created_at TIMESTAMP,
    ADD COLUMN backup_description VARCHAR(500);

CREATE INDEX idx_save_slots_format_version
    ON save_slots(format_version);
