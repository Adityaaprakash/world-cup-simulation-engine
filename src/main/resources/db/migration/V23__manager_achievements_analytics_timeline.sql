CREATE TABLE manager_achievements (
    id BIGSERIAL PRIMARY KEY,
    manager_id BIGINT NOT NULL REFERENCES managers(id),
    achievement_code VARCHAR(50) NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(300) NOT NULL,
    badge VARCHAR(20) NOT NULL,
    unlocked_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_manager_achievement_code
        UNIQUE (manager_id, achievement_code)
);

CREATE TABLE manager_career_analytics (
    id BIGSERIAL PRIMARY KEY,
    manager_id BIGINT NOT NULL UNIQUE REFERENCES managers(id),
    win_percentage DOUBLE PRECISION NOT NULL,
    average_goals_scored DOUBLE PRECISION NOT NULL,
    average_goals_conceded DOUBLE PRECISION NOT NULL,
    average_possession DOUBLE PRECISION NOT NULL,
    favorite_formation VARCHAR(50) NOT NULL,
    favorite_tactics VARCHAR(80) NOT NULL,
    tactical_profile VARCHAR(30) NOT NULL,
    most_used_lineup VARCHAR(500) NOT NULL,
    most_selected_captain VARCHAR(120) NOT NULL,
    most_trusted_players VARCHAR(500) NOT NULL,
    longest_unbeaten_streak INTEGER NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE career_timeline_events (
    id BIGSERIAL PRIMARY KEY,
    manager_id BIGINT NOT NULL REFERENCES managers(id),
    event_type VARCHAR(30) NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL,
    tournament_id BIGINT,
    team_id BIGINT,
    occurred_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_career_timeline_manager_occurred
    ON career_timeline_events(manager_id, occurred_at DESC);
