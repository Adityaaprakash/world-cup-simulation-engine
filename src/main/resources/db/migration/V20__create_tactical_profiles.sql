CREATE TABLE tactical_profiles
(
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL UNIQUE,
    attack_width INTEGER NOT NULL DEFAULT 50 CHECK (attack_width BETWEEN 1 AND 100),
    defensive_width INTEGER NOT NULL DEFAULT 50 CHECK (defensive_width BETWEEN 1 AND 100),
    defensive_line INTEGER NOT NULL DEFAULT 50 CHECK (defensive_line BETWEEN 1 AND 100),
    pressing_intensity INTEGER NOT NULL DEFAULT 50 CHECK (pressing_intensity BETWEEN 1 AND 100),
    build_up_style VARCHAR(30) NOT NULL DEFAULT 'BALANCED',
    chance_creation VARCHAR(30) NOT NULL DEFAULT 'BALANCED',
    attacking_width INTEGER NOT NULL DEFAULT 50 CHECK (attacking_width BETWEEN 1 AND 100),
    cross_frequency INTEGER NOT NULL DEFAULT 50 CHECK (cross_frequency BETWEEN 1 AND 100),
    long_ball_frequency INTEGER NOT NULL DEFAULT 50 CHECK (long_ball_frequency BETWEEN 1 AND 100),
    passing_risk INTEGER NOT NULL DEFAULT 50 CHECK (passing_risk BETWEEN 1 AND 100),
    counter_attack BOOLEAN NOT NULL DEFAULT FALSE,
    high_press BOOLEAN NOT NULL DEFAULT FALSE,
    offside_trap BOOLEAN NOT NULL DEFAULT FALSE,
    time_wasting BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_tactical_profiles_team FOREIGN KEY (team_id) REFERENCES teams(id)
);

INSERT INTO tactical_profiles (team_id)
SELECT id FROM teams
ON CONFLICT (team_id) DO NOTHING;
