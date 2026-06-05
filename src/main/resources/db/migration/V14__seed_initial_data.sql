-- Countries

INSERT INTO countries
(name, fifa_code, continent, fifa_ranking, overall_rating)
VALUES
    ('Argentina', 'ARG', 'SOUTH_AMERICA', 1, 91),
    ('France', 'FRA', 'EUROPE', 2, 90),
    ('Spain', 'ESP', 'EUROPE', 3, 89),
    ('Brazil', 'BRA', 'SOUTH_AMERICA', 4, 90),
    ('England', 'ENG', 'EUROPE', 5, 89);

-- Teams

INSERT INTO teams
(country_id, name, manager, overall_rating)
VALUES
    (1, 'Argentina National Team', 'Lionel Scaloni', 91),
    (2, 'France National Team', 'Didier Deschamps', 90),
    (3, 'Spain National Team', 'Luis de la Fuente', 89),
    (4, 'Brazil National Team', 'Carlo Ancelotti', 90),
    (5, 'England National Team', 'Thomas Tuchel', 89);

-- Argentina Players

INSERT INTO players
(country_id, name, age, position, overall_rating,
 pace, shooting, passing, dribbling,
 defending, physical, potential, market_value)
VALUES
    (1, 'Lionel Messi', 39, 'RW', 90,
     80, 90, 91, 93,
     35, 65, 90, 20000000),

    (1, 'Julian Alvarez', 26, 'ST', 86,
     84, 85, 79, 84,
     55, 76, 89, 90000000),

    (1, 'Enzo Fernandez', 25, 'CM', 87,
     75, 78, 88, 84,
     74, 80, 90, 85000000);

-- France Players

INSERT INTO players
(country_id, name, age, position, overall_rating,
 pace, shooting, passing, dribbling,
 defending, physical, potential, market_value)
VALUES
    (2, 'Kylian Mbappe', 27, 'ST', 92,
     97, 91, 82, 92,
     40, 78, 94, 180000000),

    (2, 'William Saliba', 25, 'CB', 88,
     82, 45, 74, 76,
     88, 86, 91, 100000000),

    (2, 'Aurelien Tchouameni', 26, 'CDM', 87,
     74, 72, 83, 80,
     84, 85, 90, 90000000);

-- Spain Players

INSERT INTO players
(country_id, name, age, position, overall_rating,
 pace, shooting, passing, dribbling,
 defending, physical, potential, market_value)
VALUES
    (3, 'Lamine Yamal', 19, 'RW', 88,
     90, 84, 82, 91,
     42, 60, 96, 150000000),

    (3, 'Pedri', 24, 'CM', 88,
     80, 76, 89, 89,
     72, 70, 92, 120000000),

    (3, 'Pau Cubarsi', 19, 'CB', 84,
     74, 40, 78, 76,
     86, 79, 94, 70000000);