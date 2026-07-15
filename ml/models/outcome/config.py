"""Central configuration for match outcome model training."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class OutcomeModelConfig:
    """Training split and XGBoost settings for the outcome classifier."""

    random_seed: int = 42
    validation_size: float = 0.20
    xgboost_parameters: dict[str, Any] = field(
        default_factory=lambda: {
            "objective": "multi:softprob",
            "eval_metric": "mlogloss",
            "n_estimators": 200,
            "max_depth": 5,
            "learning_rate": 0.05,
            "subsample": 0.8,
            "colsample_bytree": 0.8,
            "n_jobs": -1,
        }
    )


DEFAULT_CONFIG = OutcomeModelConfig()
