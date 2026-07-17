"""Runtime configuration for the standalone prediction API."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class ApiConfig:
    """API identity, listener settings, and model artifact locations."""

    api_version: str = "1.0.0"
    host: str = "127.0.0.1"
    port: int = 8000
    outcome_model_path: Path = Path(__file__).resolve().parents[1] / "models" / "artifacts" / "outcome_model.pkl"
    home_goal_model_path: Path = Path(__file__).resolve().parents[1] / "models" / "artifacts" / "home_goal_model.pkl"
    away_goal_model_path: Path = Path(__file__).resolve().parents[1] / "models" / "artifacts" / "away_goal_model.pkl"


DEFAULT_CONFIG = ApiConfig()
