CREATE TABLE matches
(
    id BIGSERIAL PRIMARY KEY,

    tournament_id BIGINT NOT NULL,

    group_id BIGINT,

    home_team_id BIGINT NOT NULL,

    away_team_id BIGINT NOT NULL,

    home_score INTEGER,

    away_score INTEGER,

    match_date TIMESTAMP NOT NULL,

    status VARCHAR(30) NOT NULL,

    CONSTRAINT fk_matches_tournament
        FOREIGN KEY (tournament_id)
            REFERENCES tournaments(id),

    CONSTRAINT fk_matches_group
        FOREIGN KEY (group_id)
            REFERENCES groups(id),

    CONSTRAINT fk_matches_home_team
        FOREIGN KEY (home_team_id)
            REFERENCES teams(id),

    CONSTRAINT fk_matches_away_team
        FOREIGN KEY (away_team_id)
            REFERENCES teams(id)
);