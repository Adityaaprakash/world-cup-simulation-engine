"""Central configuration for expected-goals regression training."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class GoalModelConfig:
    """Data split and XGBoost Regressor parameters shared by both goal models."""

    random_seed: int = 42
    validation_size: float = 0.20
    xgboost_parameters: dict[str, Any] = field(
        default_factory=lambda: {
            "objective": "reg:squarederror",
            "n_estimators": 200,
            "max_depth": 5,
            "learning_rate": 0.05,
            "subsample": 0.8,
            "colsample_bytree": 0.8,
            "n_jobs": -1,
        }
    )


DEFAULT_CONFIG = GoalModelConfig()
