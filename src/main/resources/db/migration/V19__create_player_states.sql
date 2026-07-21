CREATE TABLE player_states
(
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL UNIQUE,
    current_form INTEGER NOT NULL DEFAULT 0 CHECK (current_form BETWEEN -10 AND 10),
    confidence INTEGER NOT NULL DEFAULT 50 CHECK (confidence BETWEEN 0 AND 100),
    fitness INTEGER NOT NULL DEFAULT 100 CHECK (fitness BETWEEN 0 AND 100),
    fatigue INTEGER NOT NULL DEFAULT 0 CHECK (fatigue BETWEEN 0 AND 100),
    morale INTEGER NOT NULL DEFAULT 50 CHECK (morale BETWEEN 0 AND 100),
    yellow_cards INTEGER NOT NULL DEFAULT 0,
    red_card_suspension INTEGER NOT NULL DEFAULT 0,
    injury_status VARCHAR(20) NOT NULL DEFAULT 'HEALTHY',
    injury_matches_remaining INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_player_states_player FOREIGN KEY (player_id) REFERENCES players(id)
);

INSERT INTO player_states (player_id)
SELECT id FROM players
ON CONFLICT (player_id) DO NOTHING;
