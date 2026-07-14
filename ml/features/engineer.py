"""Create leakage-safe football match features from chronological match history."""

from __future__ import annotations

import numpy as np
import pandas as pd


REQUIRED_COLUMNS = {"match_date", "home_team", "away_team", "home_score", "away_score"}
FORM_COLUMNS = ("wins", "goals", "goal_difference", "goals_conceded")


def _validate_matches(matches: pd.DataFrame) -> None:
    missing = REQUIRED_COLUMNS.difference(matches.columns)
    if missing:
        raise ValueError(f"Match dataset is missing required columns: {', '.join(sorted(missing))}")


def _result(home_score: int, away_score: int) -> str:
    if home_score > away_score:
        return "HOME_WIN"
    if home_score < away_score:
        return "AWAY_WIN"
    return "DRAW"


def _team_history(matches: pd.DataFrame) -> pd.DataFrame:
    """Build one prior-performance record per team per match."""
    source = matches.copy()
    source["_match_id"] = np.arange(len(source))

    home = pd.DataFrame(
        {
            "_match_id": source["_match_id"],
            "match_date": source["match_date"],
            "team": source["home_team"],
            "goals": source["home_score"],
            "goals_conceded": source["away_score"],
        }
    )
    away = pd.DataFrame(
        {
            "_match_id": source["_match_id"],
            "match_date": source["match_date"],
            "team": source["away_team"],
            "goals": source["away_score"],
            "goals_conceded": source["home_score"],
        }
    )
    history = pd.concat([home, away], ignore_index=True)
    history["goal_difference"] = history["goals"] - history["goals_conceded"]
    history["wins"] = (history["goal_difference"] > 0).astype(int)
    # Date and match id establish a deterministic timeline for same-day matches.
    return history.sort_values(["team", "match_date", "_match_id"], kind="stable")


def _rolling_form(matches: pd.DataFrame, window: int = 5) -> pd.DataFrame:
    """Calculate form using only matches that precede each team's current match."""
    history = _team_history(matches)
    for column in FORM_COLUMNS:
        history[f"last{window}_{column}"] = history.groupby("team", sort=False)[column].transform(
            lambda series: series.shift(1).rolling(window=window, min_periods=1).sum()
        )
    history[f"matches_before_current"] = history.groupby("team", sort=False).cumcount()
    return history


def _attach_side_form(features: pd.DataFrame, history: pd.DataFrame, side: str, window: int) -> pd.DataFrame:
    """Attach one team's history to its home or away record."""
    team_column = f"{side}_team"
    selection = history[["_match_id", "team", "matches_before_current", *[f"last{window}_{name}" for name in FORM_COLUMNS]]]
    selection = selection.rename(columns={
        "team": team_column,
        "matches_before_current": f"{side}_matches_before_current",
        **{f"last{window}_{name}": f"{side}_last{window}_{name}" for name in FORM_COLUMNS},
    })
    return features.merge(selection, on=["_match_id", team_column], how="left", validate="one_to_one")


def _add_strength_features(features: pd.DataFrame, window: int) -> pd.DataFrame:
    """Derive team-strength proxies from historical scoring and concession rates."""
    for side in ("home", "away"):
        matches_played = features[f"{side}_matches_before_current"].clip(upper=window)
        denominator = matches_played.replace(0, np.nan)
        attack = features[f"{side}_last{window}_goals"].div(denominator)
        conceded = features[f"{side}_last{window}_goals_conceded"].div(denominator)
        goal_difference = features[f"{side}_last{window}_goal_difference"].div(denominator)

        features[f"{side}_attack_strength"] = attack.fillna(0.0)
        features[f"{side}_midfield_strength"] = goal_difference.fillna(0.0)
        features[f"{side}_defense_strength"] = (-conceded).fillna(0.0)
        features[f"{side}_goalkeeper_strength"] = (1.0 / (1.0 + conceded)).fillna(0.0)
    return features


def engineer_features(matches: pd.DataFrame, form_window: int = 5) -> pd.DataFrame:
    """Return match features, targets, and no future-derived historical information."""
    _validate_matches(matches)
    features = matches.copy()
    features["match_date"] = pd.to_datetime(features["match_date"], errors="coerce")
    features = features.dropna(subset=["match_date"]).sort_values("match_date", kind="stable").reset_index(drop=True)
    features["_match_id"] = np.arange(len(features))
    features["home_score"] = pd.to_numeric(features["home_score"], errors="raise").astype(int)
    features["away_score"] = pd.to_numeric(features["away_score"], errors="raise").astype(int)

    features["match_result"] = [
        _result(home, away) for home, away in zip(features["home_score"], features["away_score"])
    ]
    features["year"] = features["match_date"].dt.year.astype(int)
    features["month"] = features["match_date"].dt.month.astype(int)
    neutral_values = (
        features["neutral"]
        if "neutral" in features
        else pd.Series(False, index=features.index)
    )
    features["neutral_ground"] = neutral_values.map(
        lambda value: str(value).strip().lower() in {"true", "1", "yes", "y"}
    ).astype(int)
    tournament_values = (
        features["tournament"]
        if "tournament" in features
        else pd.Series("Unknown", index=features.index)
    )
    features["tournament_type"] = tournament_values.fillna("Unknown")

    if {"home_fifa_rank", "away_fifa_rank"}.issubset(features.columns):
        features["home_fifa_rank"] = pd.to_numeric(features["home_fifa_rank"], errors="coerce")
        features["away_fifa_rank"] = pd.to_numeric(features["away_fifa_rank"], errors="coerce")
        features["fifa_rank_difference"] = features["away_fifa_rank"] - features["home_fifa_rank"]

    history = _rolling_form(features, form_window)
    features = _attach_side_form(features, history, "home", form_window)
    features = _attach_side_form(features, history, "away", form_window)
    features = _add_strength_features(features, form_window)

    form_features = [column for column in features if f"last{form_window}_" in column]
    features[form_features] = features[form_features].fillna(0.0)
    return features.drop(columns=["_match_id", "home_matches_before_current", "away_matches_before_current"])
