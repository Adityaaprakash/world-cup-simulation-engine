"""Reusable cleanup functions for international football match datasets."""

from __future__ import annotations

import pandas as pd

from downloader import standardize_columns


REQUIRED_MATCH_COLUMNS = ("match_date", "home_team", "away_team", "home_score", "away_score")
COUNTRY_NAME_ALIASES = {
    "usa": "United States",
    "u.s.a.": "United States",
    "england": "England",
    "korea republic": "South Korea",
    "korea, south": "South Korea",
    "czech republic": "Czechia",
    "ivory coast": "Côte d'Ivoire",
}


def normalize_whitespace(dataset: pd.DataFrame) -> pd.DataFrame:
    """Trim and collapse whitespace in all textual columns."""
    cleaned = dataset.copy()
    for column in cleaned.select_dtypes(include="object"):
        cleaned[column] = cleaned[column].map(
            lambda value: " ".join(value.split()) if isinstance(value, str) else value
        )
    return cleaned


def normalize_country_names(dataset: pd.DataFrame) -> pd.DataFrame:
    """Apply known country aliases to the home and away team columns."""
    cleaned = dataset.copy()
    for column in ("home_team", "away_team"):
        if column in cleaned:
            cleaned[column] = cleaned[column].replace(COUNTRY_NAME_ALIASES)
    return cleaned


def clean_matches(dataset: pd.DataFrame) -> pd.DataFrame:
    """Clean one source dataset and retain only completed, valid matches."""
    cleaned = normalize_whitespace(standardize_columns(dataset))
    missing_columns = set(REQUIRED_MATCH_COLUMNS).difference(cleaned.columns)
    if missing_columns:
        missing = ", ".join(sorted(missing_columns))
        raise ValueError(f"Dataset is missing required match columns: {missing}")

    cleaned["match_date"] = pd.to_datetime(cleaned["match_date"], errors="coerce")
    for column in ("home_score", "away_score"):
        cleaned[column] = pd.to_numeric(cleaned[column], errors="coerce")

    # Completed matches require valid non-negative whole-number scores.
    valid_scores = (
        cleaned["home_score"].notna()
        & cleaned["away_score"].notna()
        & cleaned["home_score"].ge(0)
        & cleaned["away_score"].ge(0)
        & cleaned["home_score"].mod(1).eq(0)
        & cleaned["away_score"].mod(1).eq(0)
    )
    cleaned = cleaned.loc[valid_scores].copy()
    cleaned[["home_score", "away_score"]] = cleaned[["home_score", "away_score"]].astype("int64")
    cleaned = cleaned.dropna(subset=["match_date", "home_team", "away_team"])
    cleaned = normalize_country_names(cleaned)
    cleaned = cleaned.drop_duplicates().reset_index(drop=True)
    return cleaned
