"""Configuration for repeatable offline hybrid-engine evaluations."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

try:
    from .calibration import CalibrationSettings, DEFAULT_CALIBRATION
except ImportError:
    from calibration import CalibrationSettings, DEFAULT_CALIBRATION


MODULE_ROOT = Path(__file__).resolve().parents[1]


@dataclass(frozen=True)
class EvaluationConfig:
    """Input, artifact, reporting, and batch settings for an evaluation run."""

    number_of_simulations: int = 1_000
    random_seed: int = 42
    participating_teams: tuple[str, ...] = ()
    tournament_mode: bool = False
    dataset_path: Path = MODULE_ROOT / "datasets" / "training_dataset.csv"
    outcome_model_path: Path = MODULE_ROOT / "models" / "artifacts" / "outcome_model.pkl"
    home_goal_model_path: Path = MODULE_ROOT / "models" / "artifacts" / "home_goal_model.pkl"
    away_goal_model_path: Path = MODULE_ROOT / "models" / "artifacts" / "away_goal_model.pkl"
    outcome_metrics_path: Path = MODULE_ROOT / "models" / "metrics" / "outcome_metrics.json"
    home_goal_metrics_path: Path = MODULE_ROOT / "models" / "metrics" / "home_goal_metrics.json"
    away_goal_metrics_path: Path = MODULE_ROOT / "models" / "metrics" / "away_goal_metrics.json"
    report_output_path: Path = MODULE_ROOT / "evaluation" / "reports" / "evaluation_report.json"
    calibration: CalibrationSettings = DEFAULT_CALIBRATION


DEFAULT_CONFIG = EvaluationConfig()
