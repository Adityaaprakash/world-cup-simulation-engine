-- Keep existing player_states and backfill to the system default manager (id=1) 
-- Ensure manager 1 exists, if not, create a placeholder admin manager
INSERT INTO managers (id, username, display_name, nationality, favorite_formation, favorite_tactical_profile, coaching_style, reputation, experience_points, level, created_at, updated_at)
SELECT 1, 'admin', 'System Admin', 'Unknown', '4-3-3', 'Balanced', 'BALANCED', 'ELITE', 0, 10, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM managers WHERE id = 1);

-- Drop unique constraint that prevents multi-tenant
ALTER TABLE player_states DROP CONSTRAINT player_states_player_id_key;

-- Add manager_id as nullable first
ALTER TABLE player_states ADD COLUMN manager_id BIGINT;

-- Backfill existing states to manager 1
UPDATE player_states SET manager_id = 1 WHERE manager_id IS NULL;

-- Enforce Not Null
ALTER TABLE player_states ALTER COLUMN manager_id SET NOT NULL;

-- Add Constraints
ALTER TABLE player_states ADD CONSTRAINT fk_player_states_manager FOREIGN KEY (manager_id) REFERENCES managers(id);
ALTER TABLE player_states ADD CONSTRAINT uk_player_states_manager_player UNIQUE (manager_id, player_id);
