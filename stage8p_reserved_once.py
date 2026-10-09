#!/usr/bin/env python3
"""Fail-closed GitHub Actions guard against opening Stage 8P reserved seeds twice.

Read the *workflow-scoped* GitHub Actions runs JSON from stdin. A new run is
permitted only when it is the sole run ever created for this workflow, is the
current manual dispatch on main, and is its first attempt. A failed/cancelled
previous run also blocks: an operator must inspect whether seeds were opened.
"""
from __future__ import annotations

import argparse
import json
import sys


def validate_first_run(payload: object, *, run_id: int, run_attempt: int) -> None:
    if type(run_id) is not int or run_id <= 0:
        raise ValueError("invalid current run id")
    if type(run_attempt) is not int or run_attempt != 1:
        raise ValueError("reserved pilot rerun forbidden: only first attempt allowed")
    if not isinstance(payload, dict):
        raise ValueError("GitHub workflow-run response is not an object")
    total = payload.get("total_count")
    runs = payload.get("workflow_runs")
    if type(total) is not int or total < 0 or not isinstance(runs, list):
        raise ValueError("missing or invalid GitHub workflow-run count/list")
    if total != len(runs):
        raise ValueError("incomplete workflow-run listing: fail closed on pagination")
    if total != 1:
        raise ValueError(f"reserved pilot requires exactly one run (itself), found {total}")
    run = runs[0]
    if not isinstance(run, dict) or type(run.get("id")) is not int:
        raise ValueError("invalid GitHub workflow-run entry")
    if run["id"] != run_id:
        raise ValueError("reserved pilot was already dispatched under another run id")
    if run.get("event") != "workflow_dispatch" or run.get("head_branch") != "main":
        raise ValueError("reserved pilot requires manual dispatch on main")
    if type(run.get("run_attempt")) is not int or run["run_attempt"] != 1:
        raise ValueError("GitHub reports a repeated attempt for the reserved pilot")
    if run.get("status") not in ("queued", "in_progress", "waiting", "requested", "pending"):
        raise ValueError("current reserved pilot is not in a running or queued state")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run-id", type=int, required=True)
    parser.add_argument("--run-attempt", type=int, required=True)
    args = parser.parse_args()
    try:
        validate_first_run(json.load(sys.stdin), run_id=args.run_id,
                           run_attempt=args.run_attempt)
    except (ValueError, json.JSONDecodeError) as exc:
        print(f"Stage 8P reserved one-shot guard BLOCKED: {exc}", file=sys.stderr)
        sys.exit(1)
    print("Stage 8P reserved one-shot guard PASSED: first manual run on main")


if __name__ == "__main__":
    main()
