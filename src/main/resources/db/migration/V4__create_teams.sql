CREATE TABLE teams
(
    id BIGSERIAL PRIMARY KEY,

    country_id BIGINT NOT NULL UNIQUE,

    name VARCHAR(100) NOT NULL,

    manager VARCHAR(100),

    overall_rating INTEGER NOT NULL,

    CONSTRAINT fk_teams_country
        FOREIGN KEY (country_id)
            REFERENCES countries(id)
);