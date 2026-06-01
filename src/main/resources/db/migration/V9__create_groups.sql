CREATE TABLE groups
(
    id BIGSERIAL PRIMARY KEY,

    tournament_id BIGINT NOT NULL,

    name VARCHAR(10) NOT NULL,

    CONSTRAINT fk_groups_tournament
        FOREIGN KEY (tournament_id)
            REFERENCES tournaments(id)
);