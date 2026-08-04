ALTER TABLE players
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN retired BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN preferred_foot VARCHAR(10) NOT NULL DEFAULT 'RIGHT';

ALTER TABLE teams
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN confederation VARCHAR(30);

CREATE INDEX idx_players_dataset_status
    ON players(active, retired);

CREATE INDEX idx_teams_active
    ON teams(active);
