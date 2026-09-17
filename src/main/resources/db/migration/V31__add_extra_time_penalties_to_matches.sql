ALTER TABLE matches ADD COLUMN went_to_extra_time BOOLEAN DEFAULT false;
ALTER TABLE matches ADD COLUMN went_to_penalties BOOLEAN DEFAULT false;
ALTER TABLE matches ADD COLUMN home_penalties_score INTEGER;
ALTER TABLE matches ADD COLUMN away_penalties_score INTEGER;
