CREATE TABLE formations
(
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(20) NOT NULL UNIQUE,

    defenders INTEGER NOT NULL,

    midfielders INTEGER NOT NULL,

    attackers INTEGER NOT NULL
);