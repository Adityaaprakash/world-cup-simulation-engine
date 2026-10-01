ALTER TABLE tactical_profiles DROP COLUMN attack_width;
ALTER TABLE tactical_profiles DROP COLUMN defensive_width;
ALTER TABLE tactical_profiles DROP COLUMN chance_creation;
ALTER TABLE tactical_profiles DROP COLUMN attacking_width;
ALTER TABLE tactical_profiles DROP COLUMN cross_frequency;
ALTER TABLE tactical_profiles DROP COLUMN long_ball_frequency;
ALTER TABLE tactical_profiles DROP COLUMN passing_risk;
ALTER TABLE tactical_profiles DROP COLUMN counter_attack;
ALTER TABLE tactical_profiles DROP COLUMN high_press;
ALTER TABLE tactical_profiles DROP COLUMN offside_trap;
ALTER TABLE tactical_profiles DROP COLUMN time_wasting;

ALTER TABLE tactical_profiles ALTER COLUMN pressing_intensity TYPE VARCHAR(30) USING 'BALANCED';
ALTER TABLE tactical_profiles ALTER COLUMN pressing_intensity SET DEFAULT 'BALANCED';

ALTER TABLE tactical_profiles ALTER COLUMN defensive_line TYPE VARCHAR(30) USING 'BALANCED';
ALTER TABLE tactical_profiles ALTER COLUMN defensive_line SET DEFAULT 'BALANCED';

ALTER TABLE tactical_profiles ADD COLUMN tempo VARCHAR(30) NOT NULL DEFAULT 'BALANCED';
ALTER TABLE tactical_profiles ADD COLUMN width VARCHAR(30) NOT NULL DEFAULT 'BALANCED';
ALTER TABLE tactical_profiles ADD COLUMN passing_style VARCHAR(30) NOT NULL DEFAULT 'MIXED';
ALTER TABLE tactical_profiles ADD COLUMN attacking_approach VARCHAR(30) NOT NULL DEFAULT 'BALANCED';
ALTER TABLE tactical_profiles ADD COLUMN defensive_block VARCHAR(30) NOT NULL DEFAULT 'MID_BLOCK';

UPDATE tactical_profiles SET build_up_style = 'POSSESSION' WHERE build_up_style = 'SLOW_POSSESSION';
