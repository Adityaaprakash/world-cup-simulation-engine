CREATE TABLE tournament_teams
(
    id BIGSERIAL PRIMARY KEY,

    tournament_id BIGINT NOT NULL,

    group_id BIGINT NOT NULL,

    team_id BIGINT NOT NULL,

    seed INTEGER NOT NULL,

    CONSTRAINT fk_tournament_teams_tournament
        FOREIGN KEY (tournament_id)
            REFERENCES tournaments(id),

    CONSTRAINT fk_tournament_teams_group
        FOREIGN KEY (group_id)
            REFERENCES groups(id),

    CONSTRAINT fk_tournament_teams_team
        FOREIGN KEY (team_id)
            REFERENCES teams(id)
);