ALTER TABLE player_states ADD COLUMN development_rating INTEGER NOT NULL DEFAULT 0 CHECK (development_rating BETWEEN -5 AND 10);
ALTER TABLE player_states ADD COLUMN progression_tracker INTEGER NOT NULL DEFAULT 0 CHECK (progression_tracker BETWEEN -100 AND 100);
