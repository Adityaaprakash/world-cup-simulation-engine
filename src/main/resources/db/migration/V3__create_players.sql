CREATE TABLE players
(
    id BIGSERIAL PRIMARY KEY,

    country_id BIGINT NOT NULL,

    name VARCHAR(100) NOT NULL,

    age INTEGER NOT NULL,

    position VARCHAR(20) NOT NULL,

    overall_rating INTEGER NOT NULL,

    pace INTEGER NOT NULL,
    shooting INTEGER NOT NULL,
    passing INTEGER NOT NULL,
    dribbling INTEGER NOT NULL,
    defending INTEGER NOT NULL,
    physical INTEGER NOT NULL,

    potential INTEGER NOT NULL,

    market_value BIGINT NOT NULL,

    CONSTRAINT fk_players_country
        FOREIGN KEY (country_id)
            REFERENCES countries(id)
);