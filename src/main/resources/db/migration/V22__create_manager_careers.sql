CREATE TABLE managers (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    nationality VARCHAR(80) NOT NULL,
    favorite_formation VARCHAR(50) NOT NULL,
    favorite_tactical_profile VARCHAR(80) NOT NULL,
    coaching_style VARCHAR(30) NOT NULL,
    reputation VARCHAR(30) NOT NULL,
    experience_points INTEGER NOT NULL,
    level INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE career_statistics (
    id BIGSERIAL PRIMARY KEY,
    manager_id BIGINT NOT NULL UNIQUE REFERENCES managers(id),
    tournaments_managed INTEGER NOT NULL,
    matches_managed INTEGER NOT NULL,
    wins INTEGER NOT NULL,
    draws INTEGER NOT NULL,
    losses INTEGER NOT NULL,
    goals_scored INTEGER NOT NULL,
    goals_conceded INTEGER NOT NULL,
    clean_sheets INTEGER NOT NULL,
    trophies_won INTEGER NOT NULL,
    finals_reached INTEGER NOT NULL,
    semi_finals_reached INTEGER NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE career_history (
    id BIGSERIAL PRIMARY KEY,
    manager_id BIGINT NOT NULL REFERENCES managers(id),
    tournament_id BIGINT NOT NULL REFERENCES tournaments(id),
    team_id BIGINT NOT NULL REFERENCES teams(id),
    finishing_position INTEGER NOT NULL,
    wins INTEGER NOT NULL,
    losses INTEGER NOT NULL,
    goals_scored INTEGER NOT NULL,
    goals_conceded INTEGER NOT NULL,
    trophies INTEGER NOT NULL,
    date_completed TIMESTAMP NOT NULL,
    CONSTRAINT uk_career_history_manager_tournament_team
        UNIQUE (manager_id, tournament_id, team_id)
);
