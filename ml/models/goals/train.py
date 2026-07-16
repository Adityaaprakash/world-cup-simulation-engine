"""Train, evaluate, and persist independent XGBoost home and away goal models."""

from __future__ import annotations

import argparse
from pathlib import Path
from typing import Any

import joblib
import pandas as pd
from sklearn.model_selection import train_test_split
from xgboost import XGBRegressor

try:
    from .config import DEFAULT_CONFIG, GoalModelConfig
    from .evaluator import calculate_metrics, save_metrics
    from .model_loader import clear_model_cache
except ImportError:  # Allows: python train.py
    from config import DEFAULT_CONFIG, GoalModelConfig
    from evaluator import calculate_metrics, save_metrics
    from model_loader import clear_model_cache


MODULE_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_DATASET_PATH = MODULE_ROOT / "datasets" / "training_dataset.csv"
DEFAULT_HOME_MODEL_PATH = MODULE_ROOT / "models" / "artifacts" / "home_goal_model.pkl"
DEFAULT_AWAY_MODEL_PATH = MODULE_ROOT / "models" / "artifacts" / "away_goal_model.pkl"
DEFAULT_HOME_METRICS_PATH = MODULE_ROOT / "models" / "metrics" / "home_goal_metrics.json"
DEFAULT_AWAY_METRICS_PATH = MODULE_ROOT / "models" / "metrics" / "away_goal_metrics.json"
TARGET_COLUMNS = ("home_score", "away_score")
EXCLUDED_COLUMNS = ("home_score", "away_score", "match_result")


def load_training_data(dataset_path: Path) -> tuple[pd.DataFrame, pd.Series, pd.Series]:
    """Load numerical features and the independent home/away goal targets."""
    source = Path(dataset_path).expanduser()
    if not source.is_file():
        raise FileNotFoundError(f"Training dataset was not found: {source}")
    dataset = pd.read_csv(source)
    missing_targets = set(TARGET_COLUMNS).difference(dataset.columns)
    if missing_targets:
        raise ValueError(f"Training dataset is missing target columns: {', '.join(sorted(missing_targets))}")

    features = dataset.drop(columns=[column for column in EXCLUDED_COLUMNS if column in dataset])
    if features.empty:
        raise ValueError("Training dataset has no input feature columns.")
    if not all(pd.api.types.is_numeric_dtype(features[column]) for column in features):
        raise ValueError("All training features must be numerical after Phase 8B encoding.")
    return features, pd.to_numeric(dataset["home_score"], errors="raise"), pd.to_numeric(dataset["away_score"], errors="raise")


def _save_model(model: XGBRegressor, feature_columns: list[str], output_path: Path) -> Path:
    """Persist one regressor together with its required feature ordering."""
    destination = Path(output_path).expanduser()
    destination.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump({"model": model, "feature_columns": feature_columns}, destination)
    return destination


def train_goal_models(
    dataset_path: Path = DEFAULT_DATASET_PATH,
    home_model_path: Path = DEFAULT_HOME_MODEL_PATH,
    away_model_path: Path = DEFAULT_AWAY_MODEL_PATH,
    home_metrics_path: Path = DEFAULT_HOME_METRICS_PATH,
    away_metrics_path: Path = DEFAULT_AWAY_METRICS_PATH,
    config: GoalModelConfig = DEFAULT_CONFIG,
) -> dict[str, dict[str, float]]:
    """Train and persist separate home- and away-goal XGBoost regressors."""
    features, home_target, away_target = load_training_data(dataset_path)
    x_train, x_validation, home_train, home_validation, away_train, away_validation = train_test_split(
        features,
        home_target,
        away_target,
        test_size=config.validation_size,
        random_state=config.random_seed,
    )
    parameters = {**config.xgboost_parameters, "random_state": config.random_seed}
    home_model = XGBRegressor(**parameters)
    away_model = XGBRegressor(**parameters)
    home_model.fit(x_train, home_train)
    away_model.fit(x_train, away_train)

    home_metrics = calculate_metrics(home_validation, home_model.predict(x_validation))
    away_metrics = calculate_metrics(away_validation, away_model.predict(x_validation))
    save_metrics(home_metrics, home_metrics_path)
    save_metrics(away_metrics, away_metrics_path)
    _save_model(home_model, features.columns.tolist(), home_model_path)
    _save_model(away_model, features.columns.tolist(), away_model_path)
    clear_model_cache()
    return {"home_goal_metrics": home_metrics, "away_goal_metrics": away_metrics}


def main() -> None:
    """Train both expected-goal regressors from the command line."""
    parser = argparse.ArgumentParser(description="Train independent expected-goal regressors.")
    parser.add_argument("--dataset", type=Path, default=DEFAULT_DATASET_PATH)
    parser.add_argument("--home-model-output", type=Path, default=DEFAULT_HOME_MODEL_PATH)
    parser.add_argument("--away-model-output", type=Path, default=DEFAULT_AWAY_MODEL_PATH)
    parser.add_argument("--home-metrics-output", type=Path, default=DEFAULT_HOME_METRICS_PATH)
    parser.add_argument("--away-metrics-output", type=Path, default=DEFAULT_AWAY_METRICS_PATH)
    arguments = parser.parse_args()
    train_goal_models(
        arguments.dataset,
        arguments.home_model_output,
        arguments.away_model_output,
        arguments.home_metrics_output,
        arguments.away_metrics_output,
    )
    print(f"Home goal model saved to {arguments.home_model_output}")
    print(f"Away goal model saved to {arguments.away_model_output}")


if __name__ == "__main__":
    main()
