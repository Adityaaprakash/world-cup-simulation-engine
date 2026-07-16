"""Reusable expected-goal predictions from the saved independent regressors."""

from __future__ import annotations

from pathlib import Path

import pandas as pd

try:
    from .model_loader import load_goal_model
except ImportError:  # Allows direct execution/import from this directory.
    from model_loader import load_goal_model


MODULE_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_HOME_MODEL_PATH = MODULE_ROOT / "models" / "artifacts" / "home_goal_model.pkl"
DEFAULT_AWAY_MODEL_PATH = MODULE_ROOT / "models" / "artifacts" / "away_goal_model.pkl"


def _prepare_features(features: pd.DataFrame, artifact: dict[str, object]) -> pd.DataFrame:
    columns = artifact["feature_columns"]
    if not isinstance(columns, list):
        raise ValueError("Goal model artifact has invalid feature metadata.")
    missing = set(columns).difference(features.columns)
    if missing:
        raise ValueError(f"Prediction features are missing columns: {', '.join(sorted(missing))}")
    return features.loc[:, columns]


def predict_expected_goals_batch(
    features: pd.DataFrame,
    home_model_path: Path = DEFAULT_HOME_MODEL_PATH,
    away_model_path: Path = DEFAULT_AWAY_MODEL_PATH,
) -> list[dict[str, float]]:
    """Predict independent expected home and away goals for multiple feature rows."""
    home_artifact = load_goal_model(home_model_path)
    away_artifact = load_goal_model(away_model_path)
    home_predictions = home_artifact["model"].predict(_prepare_features(features, home_artifact))
    away_predictions = away_artifact["model"].predict(_prepare_features(features, away_artifact))
    return [
        {
            "expected_home_goals": float(max(0.0, home_goals)),
            "expected_away_goals": float(max(0.0, away_goals)),
        }
        for home_goals, away_goals in zip(home_predictions, away_predictions)
    ]


def predict_expected_goals(
    features: pd.DataFrame,
    home_model_path: Path = DEFAULT_HOME_MODEL_PATH,
    away_model_path: Path = DEFAULT_AWAY_MODEL_PATH,
) -> dict[str, float]:
    """Return expected home and away goals for exactly one match feature row."""
    if len(features) != 1:
        raise ValueError("Use predict_expected_goals_batch for multiple rows.")
    return predict_expected_goals_batch(features, home_model_path, away_model_path)[0]
