-- Phase 14C: Add isolated attribute deltas to player_states for independent save-slot progression
ALTER TABLE player_states 
    ADD COLUMN pace_delta INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN shooting_delta INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN passing_delta INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN dribbling_delta INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN defending_delta INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN physical_delta INTEGER NOT NULL DEFAULT 0;
