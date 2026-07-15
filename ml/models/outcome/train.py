"""Train, evaluate, and persist the XGBoost match outcome classifier."""

from __future__ import annotations

import argparse
from pathlib import Path
from typing import Any

import joblib
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from xgboost import XGBClassifier

try:
    from .config import DEFAULT_CONFIG, OutcomeModelConfig
    from .evaluator import calculate_metrics, save_metrics
    from .model_loader import clear_model_cache
except ImportError:  # Allows: python train.py
    from config import DEFAULT_CONFIG, OutcomeModelConfig
    from evaluator import calculate_metrics, save_metrics
    from model_loader import clear_model_cache


MODULE_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_DATASET_PATH = MODULE_ROOT / "datasets" / "training_dataset.csv"
DEFAULT_MODEL_PATH = MODULE_ROOT / "models" / "artifacts" / "outcome_model.pkl"
DEFAULT_METRICS_PATH = MODULE_ROOT / "models" / "metrics" / "outcome_metrics.json"
TARGET_COLUMN = "match_result"
EXCLUDED_COLUMNS = ("match_result", "home_score", "away_score")


def load_training_data(dataset_path: Path) -> tuple[pd.DataFrame, pd.Series]:
    """Load training data and separate target from automatically selected features."""
    source = Path(dataset_path).expanduser()
    if not source.is_file():
        raise FileNotFoundError(f"Training dataset was not found: {source}")
    dataset = pd.read_csv(source)
    if TARGET_COLUMN not in dataset:
        raise ValueError(f"Training dataset is missing target column: {TARGET_COLUMN}")

    features = dataset.drop(columns=[column for column in EXCLUDED_COLUMNS if column in dataset])
    if features.empty:
        raise ValueError("Training dataset has no input feature columns.")
    if not all(pd.api.types.is_numeric_dtype(features[column]) for column in features):
        raise ValueError("All training features must be numerical after Phase 8B encoding.")
    return features, dataset[TARGET_COLUMN].astype(str)


def train_outcome_model(
    dataset_path: Path = DEFAULT_DATASET_PATH,
    model_path: Path = DEFAULT_MODEL_PATH,
    metrics_path: Path = DEFAULT_METRICS_PATH,
    config: OutcomeModelConfig = DEFAULT_CONFIG,
) -> dict[str, Any]:
    """Train XGBoost, persist its artifact, and save basic validation metrics."""
    features, target = load_training_data(dataset_path)
    label_encoder = LabelEncoder()
    encoded_target = label_encoder.fit_transform(target)
    class_counts = pd.Series(encoded_target).value_counts()
    stratify_target = encoded_target if class_counts.min() >= 2 else None
    x_train, x_validation, y_train, y_validation = train_test_split(
        features,
        encoded_target,
        test_size=config.validation_size,
        random_state=config.random_seed,
        stratify=stratify_target,
    )

    parameters = {**config.xgboost_parameters, "random_state": config.random_seed, "num_class": len(label_encoder.classes_)}
    model = XGBClassifier(**parameters)
    model.fit(x_train, y_train)

    validation_predictions = model.predict(x_validation)
    actual_labels = label_encoder.inverse_transform(y_validation)
    predicted_labels = label_encoder.inverse_transform(validation_predictions.astype(int))
    metrics = calculate_metrics(actual_labels, predicted_labels, label_encoder.classes_.tolist())
    save_metrics(metrics, metrics_path)

    destination = Path(model_path).expanduser()
    destination.parent.mkdir(parents=True, exist_ok=True)
    artifact = {
        "model": model,
        "label_encoder": label_encoder,
        "feature_columns": features.columns.tolist(),
    }
    joblib.dump(artifact, destination)
    clear_model_cache()
    return metrics


def main() -> None:
    """Train the outcome model from the command line."""
    parser = argparse.ArgumentParser(description="Train the match outcome classifier.")
    parser.add_argument("--dataset", type=Path, default=DEFAULT_DATASET_PATH)
    parser.add_argument("--model-output", type=Path, default=DEFAULT_MODEL_PATH)
    parser.add_argument("--metrics-output", type=Path, default=DEFAULT_METRICS_PATH)
    arguments = parser.parse_args()
    metrics = train_outcome_model(arguments.dataset, arguments.model_output, arguments.metrics_output)
    print(f"Outcome model saved to {arguments.model_output}")
    print(f"Validation accuracy: {metrics['accuracy']:.4f}")


if __name__ == "__main__":
    main()
