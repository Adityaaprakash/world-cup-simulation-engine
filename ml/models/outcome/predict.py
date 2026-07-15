"""Reusable probability predictions for the saved match outcome model."""

from __future__ import annotations

from pathlib import Path

import pandas as pd

try:
    from .model_loader import load_outcome_model
except ImportError:  # Allows direct execution/import from this directory.
    from model_loader import load_outcome_model


CLASS_TO_RESPONSE_KEY = {
    "HOME_WIN": "home_win_probability",
    "DRAW": "draw_probability",
    "AWAY_WIN": "away_win_probability",
}


def predict_outcome_probabilities(features: pd.DataFrame, model_path: Path) -> list[dict[str, float]]:
    """Predict home-win, draw, and away-win probabilities for each feature row."""
    artifact = load_outcome_model(model_path)
    feature_columns: list[str] = artifact["feature_columns"]
    missing = set(feature_columns).difference(features.columns)
    if missing:
        raise ValueError(f"Prediction features are missing columns: {', '.join(sorted(missing))}")

    probabilities = artifact["model"].predict_proba(features.loc[:, feature_columns])
    class_names = artifact["label_encoder"].inverse_transform(artifact["model"].classes_)
    results: list[dict[str, float]] = []
    for row in probabilities:
        result = {key: 0.0 for key in CLASS_TO_RESPONSE_KEY.values()}
        for class_name, probability in zip(class_names, row):
            result[CLASS_TO_RESPONSE_KEY[str(class_name)]] = float(probability)
        results.append(result)
    return results


def predict_outcome_probability(features: pd.DataFrame, model_path: Path) -> dict[str, float]:
    """Predict probabilities for exactly one match feature row."""
    if len(features) != 1:
        raise ValueError("Use predict_outcome_probabilities for multiple rows.")
    return predict_outcome_probabilities(features, model_path)[0]
