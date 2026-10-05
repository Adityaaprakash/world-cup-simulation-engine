ALTER TABLE save_slots ADD COLUMN initial_season INTEGER DEFAULT 2026;
UPDATE save_slots SET initial_season = current_season WHERE initial_season IS NULL;
ALTER TABLE save_slots ALTER COLUMN initial_season SET NOT NULL;

ALTER TABLE players ADD COLUMN generated_manager_id BIGINT;
ALTER TABLE players ADD CONSTRAINT fk_players_manager FOREIGN KEY (generated_manager_id) REFERENCES managers(id) ON DELETE CASCADE;
