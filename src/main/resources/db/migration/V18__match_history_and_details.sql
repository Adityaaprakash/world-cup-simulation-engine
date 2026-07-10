ALTER TABLE matches ADD COLUMN man_of_the_match_player_id BIGINT;
ALTER TABLE matches ADD CONSTRAINT fk_matches_man_of_the_match FOREIGN KEY (man_of_the_match_player_id) REFERENCES players(id);

CREATE TABLE match_statistics
(
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT UNIQUE NOT NULL,
    home_possession INTEGER NOT NULL,
    away_possession INTEGER NOT NULL,
    home_shots INTEGER NOT NULL,
    away_shots INTEGER NOT NULL,
    home_shots_on_target INTEGER NOT NULL,
    away_shots_on_target INTEGER NOT NULL,
    home_passes INTEGER NOT NULL,
    away_passes INTEGER NOT NULL,
    home_pass_accuracy INTEGER NOT NULL,
    away_pass_accuracy INTEGER NOT NULL,
    home_corners INTEGER NOT NULL,
    away_corners INTEGER NOT NULL,
    home_fouls INTEGER NOT NULL,
    away_fouls INTEGER NOT NULL,
    home_offsides INTEGER NOT NULL,
    away_offsides INTEGER NOT NULL,
    home_yellow_cards INTEGER NOT NULL,
    away_yellow_cards INTEGER NOT NULL,
    home_red_cards INTEGER NOT NULL,
    away_red_cards INTEGER NOT NULL,
    home_saves INTEGER NOT NULL,
    away_saves INTEGER NOT NULL,
    home_expected_goals DOUBLE PRECISION NOT NULL,
    away_expected_goals DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_match_statistics_match FOREIGN KEY (match_id) REFERENCES matches(id)
);

CREATE TABLE player_match_ratings
(
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    rating DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_player_match_ratings_match FOREIGN KEY (match_id) REFERENCES matches(id),
    CONSTRAINT fk_player_match_ratings_player FOREIGN KEY (player_id) REFERENCES players(id),
    CONSTRAINT uq_player_match_rating UNIQUE (match_id, player_id)
);
