"""Offline batch simulator using the trained prediction artifacts when available."""

from __future__ import annotations

import logging
from dataclasses import dataclass

import numpy as np
import pandas as pd

try:
    from ..models.goals.predict import predict_expected_goals
    from ..models.outcome.predict import predict_outcome_probability
    from .calibration import CalibrationSettings
    from .config import EvaluationConfig
except ImportError:
    from models.goals.predict import predict_expected_goals
    from models.outcome.predict import predict_outcome_probability
    from calibration import CalibrationSettings
    from config import EvaluationConfig


LOGGER = logging.getLogger(__name__)


@dataclass(frozen=True)
class SimulationBatch:
    """Scoreline results and robustness counters from an evaluation batch."""

    matches: pd.DataFrame
    ml_available: bool
    fallback_usage_count: int
    prediction_failure_count: int
    invalid_feature_count: int


def simulate_matches(features: pd.DataFrame, config: EvaluationConfig) -> SimulationBatch:
    """Sample match outcomes and scores without changing production simulation code."""
    if features.empty:
        raise ValueError("Cannot evaluate an empty training dataset.")
    if config.number_of_simulations < 1:
        raise ValueError("number_of_simulations must be at least 1.")

    rng = np.random.default_rng(config.random_seed)
    sampled_indices = rng.integers(0, len(features), size=config.number_of_simulations)
    ml_available = all(
        path.is_file()
        for path in (config.outcome_model_path, config.home_goal_model_path, config.away_goal_model_path)
    )
    fallback_usage_count = 0
    prediction_failure_count = 0
    invalid_feature_count = 0
    results: list[dict[str, int | bool]] = []

    LOGGER.info("Starting hybrid evaluation with %s simulations.", config.number_of_simulations)
    for index in sampled_indices:
        row = features.iloc[[int(index)]]
        try:
            if not ml_available:
                raise FileNotFoundError("Model artifacts are unavailable.")
            outcome = predict_outcome_probability(row, config.outcome_model_path)
            goals = predict_expected_goals(row, config.home_goal_model_path, config.away_goal_model_path)
            home_goals, away_goals = _sample_ml_scoreline(outcome, goals, rng, config.calibration)
            used_fallback = False
        except ValueError:
            invalid_feature_count += 1
            fallback_usage_count += 1
            home_goals, away_goals = _fallback_scoreline(row, rng, config.calibration)
            used_fallback = True
        except Exception:
            prediction_failure_count += 1
            fallback_usage_count += 1
            home_goals, away_goals = _fallback_scoreline(row, rng, config.calibration)
            used_fallback = True

        results.append({"home_goals": home_goals, "away_goals": away_goals, "used_fallback": used_fallback})

    LOGGER.info("Hybrid evaluation complete; fallback used %s times.", fallback_usage_count)
    return SimulationBatch(
        matches=pd.DataFrame(results),
        ml_available=ml_available,
        fallback_usage_count=fallback_usage_count,
        prediction_failure_count=prediction_failure_count,
        invalid_feature_count=invalid_feature_count,
    )


def _sample_ml_scoreline(
    outcome: dict[str, float], goals: dict[str, float], rng: np.random.Generator, calibration: CalibrationSettings
) -> tuple[int, int]:
    probabilities = np.array([
        outcome["home_win_probability"],
        outcome["draw_probability"] * calibration.draw_adjustment_factor,
        outcome["away_win_probability"],
    ])
    probabilities = np.clip(probabilities, 0.0, None)
    probabilities = probabilities / probabilities.sum() if probabilities.sum() else np.array([0.4, 0.2, 0.4])
    selected_outcome = int(rng.choice(3, p=probabilities))
    home_goals = _goals_around(goals["expected_home_goals"], rng, calibration)
    away_goals = _goals_around(goals["expected_away_goals"], rng, calibration)

    if selected_outcome == 0 and home_goals <= away_goals:
        home_goals = min(8, away_goals + 1)
    elif selected_outcome == 2 and away_goals <= home_goals:
        away_goals = min(8, home_goals + 1)
    elif selected_outcome == 1 and home_goals != away_goals:
        home_goals = away_goals = int(round((home_goals + away_goals) / 2))
    return home_goals, away_goals


def _goals_around(expected_goals: float, rng: np.random.Generator, calibration: CalibrationSettings) -> int:
    baseline = float(np.clip(expected_goals, calibration.minimum_expected_goals, calibration.maximum_expected_goals))
    variance = np.sqrt(max(0.5, baseline)) * calibration.goal_variance_multiplier
    return int(np.clip(round(rng.normal(baseline, variance)), 0, 8))


def _fallback_scoreline(
    features: pd.DataFrame, rng: np.random.Generator, calibration: CalibrationSettings
) -> tuple[int, int]:
    home_attack = float(features.get("home_attack_strength", pd.Series([0.0])).iloc[0])
    away_attack = float(features.get("away_attack_strength", pd.Series([0.0])).iloc[0])
    home_expected = 1.25 + home_attack * calibration.team_strength_influence_weight
    away_expected = 1.10 + away_attack * calibration.team_strength_influence_weight
    return _goals_around(home_expected, rng, calibration), _goals_around(away_expected, rng, calibration)
