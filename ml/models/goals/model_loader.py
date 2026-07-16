"""Cached loading for trusted, locally generated goal-model artifacts."""

from __future__ import annotations

from functools import lru_cache
from pathlib import Path
from typing import Any

import joblib


@lru_cache(maxsize=4)
def _load_cached(resolved_path: str) -> dict[str, Any]:
    artifact = joblib.load(resolved_path)
    required_keys = {"model", "feature_columns"}
    if not isinstance(artifact, dict) or not required_keys.issubset(artifact):
        raise ValueError("Invalid goal-model artifact structure.")
    return artifact


def load_goal_model(model_path: Path) -> dict[str, Any]:
    """Load and cache one trusted locally generated goal-model artifact."""
    path = Path(model_path).expanduser().resolve()
    if not path.is_file():
        raise FileNotFoundError(f"Goal model artifact was not found: {path}")
    return _load_cached(str(path))


def clear_model_cache() -> None:
    """Clear cached goal models after retraining in the same process."""
    _load_cached.cache_clear()
