"""Mutation-proof the inherited assertions using their declaring Stage10S class."""
import argparse
from pathlib import Path
from forge_tests.stage10s_rejection_integrity import run
from forge_tests.verify_stage10t_surefire import verify

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--forge-root", type=Path, required=True)
    parser.add_argument("--baseline", type=Path, required=True)
    parser.add_argument("--evidence", type=Path, required=True)
    args = parser.parse_args()
    run(args.forge_root, args.baseline, args.evidence, verify_baseline=verify)
