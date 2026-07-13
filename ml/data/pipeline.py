"""Executable pipeline for producing the unified football match dataset."""

from __future__ import annotations

import argparse
from pathlib import Path

from cleaner import clean_matches
from downloader import find_csv_files, load_csv_files
from merger import merge_datasets


MODULE_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_RAW_DIRECTORY = MODULE_ROOT / "data" / "raw"
DEFAULT_OUTPUT_PATH = MODULE_ROOT / "datasets" / "matches.csv"


def run_pipeline(raw_directory: Path, output_path: Path) -> Path:
    """Load, clean, merge, and save all CSV datasets in ``raw_directory``."""
    source_files = find_csv_files(raw_directory)
    if not source_files:
        raise FileNotFoundError(f"No CSV datasets found in: {raw_directory}")

    datasets = load_csv_files(source_files)
    merged = merge_datasets(clean_matches(dataset) for dataset in datasets)
    destination = Path(output_path).expanduser()
    destination.parent.mkdir(parents=True, exist_ok=True)
    merged.to_csv(destination, index=False, date_format="%Y-%m-%d")
    return destination


def main() -> None:
    """Run the pipeline from the command line."""
    parser = argparse.ArgumentParser(description="Prepare football match datasets.")
    parser.add_argument("--raw-dir", type=Path, default=DEFAULT_RAW_DIRECTORY)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT_PATH)
    arguments = parser.parse_args()

    output = run_pipeline(arguments.raw_dir, arguments.output)
    print(f"Processed dataset written to {output}")


if __name__ == "__main__":
    main()
