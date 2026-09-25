CREATE TABLE match_plans
(
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL,
    squad_id BIGINT NOT NULL,
    manager_id BIGINT NOT NULL,
    tactical_approach VARCHAR(30) NOT NULL DEFAULT 'BALANCED',
    pressing_intensity INTEGER NOT NULL DEFAULT 50 CHECK (pressing_intensity BETWEEN 1 AND 100),
    tempo INTEGER NOT NULL DEFAULT 50 CHECK (tempo BETWEEN 1 AND 100),
    defensive_line INTEGER NOT NULL DEFAULT 50 CHECK (defensive_line BETWEEN 1 AND 100),
    attacking_width INTEGER NOT NULL DEFAULT 50 CHECK (attacking_width BETWEEN 1 AND 100),
    counter_attack BOOLEAN NOT NULL DEFAULT FALSE,
    offside_trap BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_match_plans_match FOREIGN KEY (match_id) REFERENCES matches (id),
    CONSTRAINT fk_match_plans_squad FOREIGN KEY (squad_id) REFERENCES squads (id),
    CONSTRAINT fk_match_plans_manager FOREIGN KEY (manager_id) REFERENCES managers (id),
    CONSTRAINT uq_match_plans_match_squad UNIQUE (match_id, squad_id)
);
