CREATE TABLE squad_players
(
    id BIGSERIAL PRIMARY KEY,

    squad_id BIGINT NOT NULL,

    player_id BIGINT NOT NULL,

    position_slot VARCHAR(10) NOT NULL,

    starting_xi BOOLEAN NOT NULL DEFAULT TRUE,

    captain BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_squad_players_squad
        FOREIGN KEY (squad_id)
            REFERENCES squads(id),

    CONSTRAINT fk_squad_players_player
        FOREIGN KEY (player_id)
            REFERENCES players(id)
);