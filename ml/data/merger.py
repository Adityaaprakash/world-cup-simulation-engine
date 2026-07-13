"""Merge cleaned football match datasets into a consistent chronological set."""

from __future__ import annotations

from collections.abc import Iterable

import pandas as pd


MATCH_IDENTITY_COLUMNS = ["match_date", "home_team", "away_team", "home_score", "away_score"]


def merge_datasets(datasets: Iterable[pd.DataFrame]) -> pd.DataFrame:
    """Align schemas, deduplicate matches, and sort the unified dataset by date."""
    frames = [dataset.copy() for dataset in datasets]
    if not frames:
        return pd.DataFrame()

    columns = list(dict.fromkeys(column for frame in frames for column in frame.columns))
    aligned = [frame.reindex(columns=columns) for frame in frames]
    merged = pd.concat(aligned, ignore_index=True)
    identity_columns = [column for column in MATCH_IDENTITY_COLUMNS if column in merged]
    merged = merged.drop_duplicates(subset=identity_columns or None)
    return merged.sort_values("match_date", kind="stable").reset_index(drop=True)
