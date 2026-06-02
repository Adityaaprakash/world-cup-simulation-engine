CREATE TABLE standings
(
    id BIGSERIAL PRIMARY KEY,

    tournament_id BIGINT NOT NULL,

    group_id BIGINT NOT NULL,

    team_id BIGINT NOT NULL,

    played INTEGER NOT NULL DEFAULT 0,
    won INTEGER NOT NULL DEFAULT 0,
    drawn INTEGER NOT NULL DEFAULT 0,
    lost INTEGER NOT NULL DEFAULT 0,

    goals_for INTEGER NOT NULL DEFAULT 0,
    goals_against INTEGER NOT NULL DEFAULT 0,

    goal_difference INTEGER NOT NULL DEFAULT 0,

    points INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_standings_tournament
        FOREIGN KEY (tournament_id)
            REFERENCES tournaments(id),

    CONSTRAINT fk_standings_group
        FOREIGN KEY (group_id)
            REFERENCES groups(id),

    CONSTRAINT fk_standings_team
        FOREIGN KEY (team_id)
            REFERENCES teams(id)
);