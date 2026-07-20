"""Tunable constants for offline hybrid-engine evaluation."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class CalibrationSettings:
    """Controls used when sampling scorelines from model predictions."""

    goal_variance_multiplier: float = 1.0
    draw_adjustment_factor: float = 1.0
    chemistry_influence_weight: float = 0.0
    team_strength_influence_weight: float = 0.01
    minimum_expected_goals: float = 0.0
    maximum_expected_goals: float = 5.0


DEFAULT_CALIBRATION = CalibrationSettings()
