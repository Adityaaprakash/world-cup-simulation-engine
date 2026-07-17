"""Typed request and response contracts for ML prediction endpoints."""

from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field


class PredictionRequest(BaseModel):
    """Numerical Phase 8B feature vector for one completed-match-style fixture."""

    model_config = ConfigDict(extra="forbid")

    home_attack_strength: float = 0.0
    away_attack_strength: float = 0.0
    home_midfield_strength: float = 0.0
    away_midfield_strength: float = 0.0
    home_defense_strength: float = 0.0
    away_defense_strength: float = 0.0
    home_goalkeeper_strength: float = Field(default=0.0, ge=0.0)
    away_goalkeeper_strength: float = Field(default=0.0, ge=0.0)
    home_fifa_rank: float = Field(default=0.0, ge=0.0)
    away_fifa_rank: float = Field(default=0.0, ge=0.0)
    fifa_rank_difference: float = 0.0
    neutral_ground: int = Field(default=0, ge=0, le=1)
    year: int = Field(default=2000, ge=1800, le=2200)
    month: int = Field(default=1, ge=1, le=12)
    home_last5_wins: float = Field(default=0.0, ge=0.0, le=5.0)
    away_last5_wins: float = Field(default=0.0, ge=0.0, le=5.0)
    home_last5_goals: float = Field(default=0.0, ge=0.0)
    away_last5_goals: float = Field(default=0.0, ge=0.0)
    home_last5_goal_difference: float = 0.0
    away_last5_goal_difference: float = 0.0
    home_last5_goals_conceded: float = Field(default=0.0, ge=0.0)
    away_last5_goals_conceded: float = Field(default=0.0, ge=0.0)
    home_team_encoded: int = Field(default=0, ge=-1)
    away_team_encoded: int = Field(default=0, ge=-1)
    tournament_encoded: int = Field(default=0, ge=-1)
    tournament_type_encoded: int = Field(default=0, ge=-1)
    venue_encoded: int = Field(default=0, ge=-1)


class PredictionResponse(BaseModel):
    """Outcome probabilities and independently predicted expected goals."""

    home_win_probability: float = Field(ge=0.0, le=1.0)
    draw_probability: float = Field(ge=0.0, le=1.0)
    away_win_probability: float = Field(ge=0.0, le=1.0)
    expected_home_goals: float = Field(ge=0.0)
    expected_away_goals: float = Field(ge=0.0)
