"""Metric calculation and persisted-model-metric loading for evaluation reports."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import pandas as pd


def load_saved_metrics(metrics_path: Path) -> dict[str, Any]:
    """Load persisted model metrics when the relevant model was trained."""
    path = Path(metrics_path).expanduser()
    if not path.is_file():
        return {}
    with path.open(encoding="utf-8") as metrics_file:
        data = json.load(metrics_file)
    return data if isinstance(data, dict) else {}


def calculate_hybrid_metrics(simulated_matches: pd.DataFrame) -> dict[str, float | int]:
    """Summarize realism-oriented aggregate statistics for sampled scorelines."""
    if simulated_matches.empty:
        return {
            "matches_simulated": 0,
            "home_win_rate": 0.0,
            "draw_rate": 0.0,
            "away_win_rate": 0.0,
            "average_goals": 0.0,
            "average_home_goals": 0.0,
            "average_away_goals": 0.0,
            "average_goal_difference": 0.0,
            "clean_sheet_rate": 0.0,
            "both_teams_scored_rate": 0.0,
            "penalty_shootout_frequency": 0.0,
        }

    home_goals = simulated_matches["home_goals"]
    away_goals = simulated_matches["away_goals"]
    return {
        "matches_simulated": int(len(simulated_matches)),
        "home_win_rate": float((home_goals > away_goals).mean() * 100),
        "draw_rate": float((home_goals == away_goals).mean() * 100),
        "away_win_rate": float((home_goals < away_goals).mean() * 100),
        "average_goals": float((home_goals + away_goals).mean()),
        "average_home_goals": float(home_goals.mean()),
        "average_away_goals": float(away_goals.mean()),
        "average_goal_difference": float((home_goals - away_goals).abs().mean()),
        "clean_sheet_rate": float(((home_goals == 0) | (away_goals == 0)).mean() * 100),
        "both_teams_scored_rate": float(((home_goals > 0) & (away_goals > 0)).mean() * 100),
        "penalty_shootout_frequency": 0.0,
    }
