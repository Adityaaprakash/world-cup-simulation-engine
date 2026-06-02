CREATE TABLE match_events
(
    id BIGSERIAL PRIMARY KEY,

    match_id BIGINT NOT NULL,

    player_id BIGINT NOT NULL,

    minute INTEGER NOT NULL,

    event_type VARCHAR(30) NOT NULL,

    description VARCHAR(255),

    CONSTRAINT fk_match_events_match
        FOREIGN KEY (match_id)
            REFERENCES matches(id),

    CONSTRAINT fk_match_events_player
        FOREIGN KEY (player_id)
            REFERENCES players(id)
);