CREATE TABLE manager_jobs (
    id BIGSERIAL PRIMARY KEY,
    manager_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    target_objective VARCHAR(30) NOT NULL,
    board_confidence DOUBLE PRECISION NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    FOREIGN KEY (manager_id) REFERENCES managers(id) ON DELETE CASCADE,
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
);
