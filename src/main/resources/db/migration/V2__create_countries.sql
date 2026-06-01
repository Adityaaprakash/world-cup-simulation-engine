CREATE TABLE countries
(
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(100) NOT NULL UNIQUE,

    fifa_code VARCHAR(3) NOT NULL UNIQUE,

    continent VARCHAR(30) NOT NULL,

    fifa_ranking INTEGER NOT NULL,

    overall_rating INTEGER NOT NULL
);