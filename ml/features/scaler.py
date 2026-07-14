"""Reusable, non-persistent scaling helpers for numerical features."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Literal

import pandas as pd


ScalerKind = Literal["standard", "minmax"]


@dataclass(frozen=True)
class FittedScaler:
    """In-memory statistics needed to apply a chosen numeric scaling strategy."""

    kind: ScalerKind
    offset: pd.Series
    scale: pd.Series


def fit_scaler(dataset: pd.DataFrame, kind: ScalerKind = "standard") -> FittedScaler:
    """Fit StandardScaler- or MinMaxScaler-equivalent statistics in memory."""
    numeric = dataset.select_dtypes(include="number")
    if kind == "standard":
        offset, scale = numeric.mean(), numeric.std(ddof=0)
    elif kind == "minmax":
        offset, scale = numeric.min(), numeric.max() - numeric.min()
    else:
        raise ValueError(f"Unsupported scaler kind: {kind}")
    return FittedScaler(kind=kind, offset=offset, scale=scale.replace(0, 1.0))


def transform_with_scaler(dataset: pd.DataFrame, scaler: FittedScaler) -> pd.DataFrame:
    """Apply an already fitted scaler to matching numerical columns only."""
    transformed = dataset.copy()
    columns = [column for column in scaler.offset.index if column in transformed]
    if scaler.kind == "standard":
        transformed[columns] = (transformed[columns] - scaler.offset[columns]) / scaler.scale[columns]
    else:
        transformed[columns] = (transformed[columns] - scaler.offset[columns]) / scaler.scale[columns]
    return transformed
