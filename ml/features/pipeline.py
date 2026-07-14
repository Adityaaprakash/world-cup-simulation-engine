"""Executable feature engineering pipeline for the match training dataset."""

from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd

try:
    from .encoder import encode_categories
    from .engineer import engineer_features
except ImportError:  # Allows execution with: python pipeline.py
    from encoder import encode_categories
    from engineer import engineer_features


MODULE_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_INPUT_PATH = MODULE_ROOT / "datasets" / "matches.csv"
DEFAULT_OUTPUT_PATH = MODULE_ROOT / "datasets" / "training_dataset.csv"
CATEGORICAL_COLUMNS = ("home_team", "away_team", "tournament", "tournament_type", "venue")


def run_pipeline(input_path: Path, output_path: Path) -> Path:
    """Transform processed matches into a numerical feature dataset and save it."""
    source = Path(input_path).expanduser()
    if not source.is_file():
        raise FileNotFoundError(f"Processed matches dataset was not found: {source}")

    features = engineer_features(pd.read_csv(source))
    encoded = encode_categories(features, CATEGORICAL_COLUMNS)
    target_columns = ["home_score", "away_score", "match_result"]
    numerical_features = [
        column
        for column in encoded.select_dtypes(include="number").columns
        if column not in target_columns
    ]
    # Dates and raw labels are represented by engineered temporal and encoded
    # features; retain only numerical inputs plus the explicitly requested targets.
    training_dataset = encoded[numerical_features + target_columns]
    destination = Path(output_path).expanduser()
    destination.parent.mkdir(parents=True, exist_ok=True)
    training_dataset.to_csv(destination, index=False, date_format="%Y-%m-%d")
    return destination


def main() -> None:
    """Run feature engineering from the command line."""
    parser = argparse.ArgumentParser(description="Create the football training dataset.")
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT_PATH)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT_PATH)
    arguments = parser.parse_args()
    output = run_pipeline(arguments.input, arguments.output)
    print(f"Training dataset written to {output}")


if __name__ == "__main__":
    main()
