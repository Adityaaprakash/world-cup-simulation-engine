CREATE TABLE manager_events (
    id BIGSERIAL PRIMARY KEY,
    manager_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL,
    context_id VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    selected_decision VARCHAR(50),
    resolution_text VARCHAR(500),
    related_player_id BIGINT,
    related_match_id BIGINT,
    related_tournament_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    resolved_at TIMESTAMP,
    CONSTRAINT fk_manager_events_manager FOREIGN KEY (manager_id) REFERENCES managers(id),
    CONSTRAINT fk_manager_events_player FOREIGN KEY (related_player_id) REFERENCES players(id),
    CONSTRAINT fk_manager_events_match FOREIGN KEY (related_match_id) REFERENCES matches(id),
    CONSTRAINT fk_manager_events_tournament FOREIGN KEY (related_tournament_id) REFERENCES tournaments(id)
);

CREATE INDEX idx_manager_events_manager_id ON manager_events(manager_id);
CREATE INDEX idx_manager_events_manager_status ON manager_events(manager_id, status);
CREATE UNIQUE INDEX idx_manager_events_context ON manager_events(manager_id, context_id);
