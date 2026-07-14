"""Feature specification for the Phase 8B training dataset."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class FeatureDefinition:
    name: str
    description: str
    data_type: str
    purpose: str


FEATURE_SCHEMA = (
    FeatureDefinition("home_attack_strength", "Prior five-match average goals scored by the home team.", "float", "Home attacking proxy."),
    FeatureDefinition("away_attack_strength", "Prior five-match average goals scored by the away team.", "float", "Away attacking proxy."),
    FeatureDefinition("home_midfield_strength", "Prior five-match average goal difference for the home team.", "float", "Home overall-control proxy."),
    FeatureDefinition("away_midfield_strength", "Prior five-match average goal difference for the away team.", "float", "Away overall-control proxy."),
    FeatureDefinition("home_defense_strength", "Negative prior five-match average goals conceded by the home team.", "float", "Home defensive proxy; larger is better."),
    FeatureDefinition("away_defense_strength", "Negative prior five-match average goals conceded by the away team.", "float", "Away defensive proxy; larger is better."),
    FeatureDefinition("home_goalkeeper_strength", "Inverse prior five-match goals-conceded rate for the home team.", "float", "Home goalkeeper proxy."),
    FeatureDefinition("away_goalkeeper_strength", "Inverse prior five-match goals-conceded rate for the away team.", "float", "Away goalkeeper proxy."),
    FeatureDefinition("fifa_rank_difference", "Away FIFA rank minus home FIFA rank, when both ranks are supplied.", "float", "Relative ranking signal."),
    FeatureDefinition("neutral_ground", "Whether the match is played at a neutral venue.", "integer", "Match-context signal."),
    FeatureDefinition("tournament_type_encoded", "Deterministic code for the tournament label.", "integer", "Tournament context."),
    FeatureDefinition("year", "Calendar year of the match.", "integer", "Temporal context."),
    FeatureDefinition("month", "Calendar month of the match.", "integer", "Seasonality context."),
    FeatureDefinition("home_last5_wins / away_last5_wins", "Wins in each team's five matches before the current fixture.", "float", "Recent form."),
    FeatureDefinition("home_last5_goals / away_last5_goals", "Goals scored in each team's five matches before the current fixture.", "float", "Recent attacking form."),
    FeatureDefinition("home_last5_goal_difference / away_last5_goal_difference", "Goal difference in each team's five matches before the current fixture.", "float", "Recent overall form."),
    FeatureDefinition("home_score / away_score", "Observed completed-match scores.", "integer", "Training targets."),
    FeatureDefinition("match_result", "HOME_WIN, DRAW, or AWAY_WIN derived from final scores.", "string", "Classification target."),
)
