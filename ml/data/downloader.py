"""Import raw football match datasets from CSV files."""

from __future__ import annotations

from collections.abc import Iterable
from pathlib import Path

import pandas as pd


# Sources can use different labels for the same match attributes. Extend this
# mapping as new sources are introduced without changing pipeline consumers.
COLUMN_ALIASES: dict[str, str] = {
    "date": "match_date",
    "match date": "match_date",
    "home team": "home_team",
    "away team": "away_team",
    "home score": "home_score",
    "away score": "away_score",
    "home_team_name": "home_team",
    "away_team_name": "away_team",
    "home_team_score": "home_score",
    "away_team_score": "away_score",
    "home fifa rank": "home_fifa_rank",
    "away fifa rank": "away_fifa_rank",
}


def normalize_column_name(column: object) -> str:
    """Convert a source column name to predictable snake_case."""
    return "_".join(str(column).strip().lower().replace("-", " ").split())


def standardize_columns(dataset: pd.DataFrame) -> pd.DataFrame:
    """Return a copy with normalized names and known source aliases applied."""
    renamed = dataset.copy()
    normalized = {column: normalize_column_name(column) for column in renamed.columns}
    renamed = renamed.rename(columns=normalized)
    aliases = {normalize_column_name(key): value for key, value in COLUMN_ALIASES.items()}
    return renamed.rename(columns=aliases)


def load_csv(file_path: Path) -> pd.DataFrame:
    """Load one CSV dataset after confirming that it exists and is a file."""
    path = Path(file_path).expanduser()
    if not path.is_file():
        raise FileNotFoundError(f"Dataset CSV was not found: {path}")

    return standardize_columns(pd.read_csv(path))


def load_csv_files(file_paths: Iterable[Path]) -> list[pd.DataFrame]:
    """Load multiple CSV datasets while keeping source order intact."""
    return [load_csv(path) for path in file_paths]


def find_csv_files(raw_directory: Path) -> list[Path]:
    """Find CSV files in a raw-data directory in deterministic filename order."""
    directory = Path(raw_directory).expanduser()
    if not directory.is_dir():
        raise FileNotFoundError(f"Raw data directory was not found: {directory}")

    return sorted(directory.glob("*.csv"))
