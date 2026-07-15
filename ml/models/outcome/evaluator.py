"""Basic classification evaluation and metrics persistence."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import numpy as np
from sklearn.metrics import accuracy_score, confusion_matrix, precision_recall_fscore_support


def calculate_metrics(actual: np.ndarray, predicted: np.ndarray, labels: list[str]) -> dict[str, Any]:
    """Calculate serializable multiclass metrics for outcome predictions."""
    precision, recall, f1_score, _ = precision_recall_fscore_support(
        actual, predicted, average="weighted", zero_division=0
    )
    return {
        "accuracy": float(accuracy_score(actual, predicted)),
        "precision": float(precision),
        "recall": float(recall),
        "f1_score": float(f1_score),
        "labels": labels,
        "confusion_matrix": confusion_matrix(actual, predicted, labels=labels).tolist(),
    }


def save_metrics(metrics: dict[str, Any], output_path: Path) -> Path:
    """Persist evaluation metrics as readable JSON."""
    destination = Path(output_path).expanduser()
    destination.parent.mkdir(parents=True, exist_ok=True)
    with destination.open("w", encoding="utf-8") as metrics_file:
        json.dump(metrics, metrics_file, indent=2)
    return destination
