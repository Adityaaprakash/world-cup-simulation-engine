# Machine Learning Data Preparation

This module prepares completed international football match records for later feature engineering and model training. It does not train or serve machine learning models.

## Structure

```
ml/
├── data/
│   ├── raw/          # Place source CSV files here
│   ├── processed/    # Reserved for intermediate data, if needed later
│   ├── downloader.py # CSV discovery, loading, and column standardization
│   ├── cleaner.py    # Reusable match validation and cleanup
│   ├── merger.py     # Schema alignment, deduplication, and date ordering
│   └── pipeline.py   # Executable orchestration entry point
├── datasets/         # Generated unified datasets
└── requirements.txt
```

## Pipeline flow

The pipeline discovers CSV files in `ml/data/raw`, standardizes their column names, cleans each dataset, merges compatible records, removes duplicate matches, orders them chronologically, and writes `ml/datasets/matches.csv`.

Each source CSV must provide `match_date`, `home_team`, `away_team`, `home_score`, and `away_score` (common aliases such as `date` and `home_team_score` are accepted). Optional fields, including `tournament`, `venue`, `neutral`, `home_fifa_rank`, and `away_fifa_rank`, are retained for future phases.

## Run

Install the dependencies, place CSV sources in `ml/data/raw`, then run from the `ml/data` directory:

```bash
python pipeline.py
```

Custom locations can be used without changing code:

```bash
python pipeline.py --raw-dir path/to/raw --output path/to/matches.csv
```
