"""Evaluation report orchestration and JSON output."""

from __future__ import annotations

import json
import logging
from pathlib import Path
from typing import Any

import pandas as pd

try:
    from .config import DEFAULT_CONFIG, EvaluationConfig
    from .metrics import calculate_hybrid_metrics, load_saved_metrics
    from .simulator import simulate_matches
except ImportError:
    from config import DEFAULT_CONFIG, EvaluationConfig
    from metrics import calculate_hybrid_metrics, load_saved_metrics
    from simulator import simulate_matches


LOGGER = logging.getLogger(__name__)


def run_evaluation(config: EvaluationConfig = DEFAULT_CONFIG) -> dict[str, Any]:
    """Run an offline batch and write the combined evaluation JSON report."""
    dataset_path = Path(config.dataset_path).expanduser()
    if not dataset_path.is_file():
        raise FileNotFoundError(f"Training dataset was not found: {dataset_path}")

    batch = simulate_matches(pd.read_csv(dataset_path), config)
    report = calculate_hybrid_metrics(batch.matches)
    outcome_metrics = load_saved_metrics(config.outcome_metrics_path)
    home_goal_metrics = load_saved_metrics(config.home_goal_metrics_path)
    away_goal_metrics = load_saved_metrics(config.away_goal_metrics_path)
    report.update({
        "ml_available": batch.ml_available,
        "fallback_usage_count": batch.fallback_usage_count,
        "prediction_failure_count": batch.prediction_failure_count,
        "invalid_feature_count": batch.invalid_feature_count,
        "outcome_accuracy": outcome_metrics.get("accuracy"),
        "outcome_precision": outcome_metrics.get("precision"),
        "outcome_recall": outcome_metrics.get("recall"),
        "outcome_f1_score": outcome_metrics.get("f1_score"),
        "goal_mae_home": home_goal_metrics.get("mae"),
        "goal_rmse_home": home_goal_metrics.get("rmse"),
        "goal_r2_home": home_goal_metrics.get("r2"),
        "goal_mae_away": away_goal_metrics.get("mae"),
        "goal_rmse_away": away_goal_metrics.get("rmse"),
        "goal_r2_away": away_goal_metrics.get("r2"),
    })
    _save_report(report, config.report_output_path)
    LOGGER.info("Evaluation report written to %s.", config.report_output_path)
    return report


def _save_report(report: dict[str, Any], output_path: Path) -> Path:
    destination = Path(output_path).expanduser()
    destination.parent.mkdir(parents=True, exist_ok=True)
    with destination.open("w", encoding="utf-8") as report_file:
        json.dump(report, report_file, indent=2)
    return destination
