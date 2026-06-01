CREATE TABLE squads
(
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    team_id BIGINT NOT NULL,

    formation_id BIGINT NOT NULL,

    name VARCHAR(100) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_squads_user
        FOREIGN KEY (user_id)
            REFERENCES users(id),

    CONSTRAINT fk_squads_team
        FOREIGN KEY (team_id)
            REFERENCES teams(id),

    CONSTRAINT fk_squads_formation
        FOREIGN KEY (formation_id)
            REFERENCES formations(id)
);