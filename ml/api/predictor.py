"""Prediction orchestration independent of FastAPI route handling."""

from __future__ import annotations

import pandas as pd

try:
    from ..models.goals.model_loader import load_goal_model
    from ..models.goals.predict import predict_expected_goals
    from ..models.outcome.model_loader import load_outcome_model
    from ..models.outcome.predict import predict_outcome_probability
    from .config import ApiConfig
    from .schemas import PredictionRequest, PredictionResponse
except ImportError:  # Allows execution from the api directory.
    from config import ApiConfig
    from schemas import PredictionRequest, PredictionResponse
    from models.goals.model_loader import load_goal_model
    from models.goals.predict import predict_expected_goals
    from models.outcome.model_loader import load_outcome_model
    from models.outcome.predict import predict_outcome_probability


class PredictionService:
    """Coordinates cached model access and one-match prediction responses."""

    def __init__(self, config: ApiConfig) -> None:
        self._config = config
        self._ready = False

    def load_models(self) -> None:
        """Prime the cached artifact loaders once during application startup."""
        load_outcome_model(self._config.outcome_model_path)
        load_goal_model(self._config.home_goal_model_path)
        load_goal_model(self._config.away_goal_model_path)
        self._ready = True

    @property
    def ready(self) -> bool:
        """Whether all required artifacts were loaded successfully."""
        return self._ready

    def predict(self, request: PredictionRequest) -> PredictionResponse:
        """Convert validated features and combine outcome and goal predictions."""
        if not self._ready:
            raise RuntimeError("Prediction models are not available.")
        features = pd.DataFrame([request.model_dump()])
        outcome = predict_outcome_probability(features, self._config.outcome_model_path)
        goals = predict_expected_goals(
            features,
            self._config.home_goal_model_path,
            self._config.away_goal_model_path,
        )
        return PredictionResponse(**outcome, **goals)
