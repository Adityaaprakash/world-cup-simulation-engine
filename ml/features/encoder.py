"""Deterministic categorical encoders for match datasets."""

from __future__ import annotations

from collections.abc import Iterable

import pandas as pd


def encode_categories(dataset: pd.DataFrame, columns: Iterable[str]) -> pd.DataFrame:
    """Replace selected categories with alphabetically ordered integer codes.

    Missing values receive ``-1``. Codes are calculated from the supplied dataset,
    making this utility deterministic while remaining free of source-specific maps.
    """
    encoded = dataset.copy()
    for column in columns:
        if column not in encoded:
            continue
        values = encoded[column].astype("string")
        categories = sorted(value for value in values.dropna().unique())
        mapping = {value: index for index, value in enumerate(categories)}
        encoded[f"{column}_encoded"] = values.map(mapping).fillna(-1).astype("int64")
        encoded = encoded.drop(columns=column)
    return encoded
