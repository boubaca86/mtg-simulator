#!/usr/bin/env python3
"""Stage 8G: verify a one-way live shadow sidecar against offline replay.

Forge remains the only process that chooses and executes actions. The sidecar
receives Forge stdout only, emits read-only recommendations, and has no input
channel back into Forge.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from stage8d_shadow_policy import (
    CAPTURE_PREFIX,
    canonical_json,
    load_checkpoint,
    observe_capture,
)
from stage8f_acceptance_audit import audit_log as audit_stage8f_log


def capture_rows(lines):
    rows = []
    for line in lines:
        if line.startswith(CAPTURE_PREFIX):
            value = json.loads(line[len(CAPTURE_PREFIX):])
            if not isinstance(value, dict):
                raise ValueError("Stage 8 capture must be a JSON object")
            rows.append(value)
    if not rows:
        raise ValueError("no Stage 8 captures in Forge log")
    return rows


def live_rows(path: Path):
    rows = []
    for lineno, raw in enumerate(path.read_text().splitlines(), start=1):
        if not raw.strip():
            continue
        value = json.loads(raw)
        if not isinstance(value, dict):
            raise ValueError(f"live sidecar line {lineno} is not a JSON object")
        if value.get("schema_version") != "stage8d-shadow-recommendation-v1":
            raise ValueError(f"wrong live recommendation schema at line {lineno}")
        if value.get("mode") != "shadow-only" or value.get("promotion_allowed") is not False:
            raise ValueError(f"live recommendation escaped shadow-only mode at line {lineno}")
        if value.get("status") == "rejected":
            raise ValueError(f"live sidecar rejected capture at line {lineno}: {value.get('reason')}")
        rows.append(value)
    if not rows:
        raise ValueError("live sidecar emitted no recommendations")
    return rows


def audit_pair(log_path: Path, sidecar_path: Path, expected_games: int, policy):
    forge = audit_stage8f_log(log_path, expected_games, policy)
    captures = capture_rows(log_path.read_text().splitlines())
    live = live_rows(sidecar_path)

    if len(live) != len(captures):
        raise ValueError(
            f"live sidecar count mismatch: captures={len(captures)} recommendations={len(live)}"
        )

    offline = [observe_capture(policy, capture) for capture in captures]
    mismatches = []
    for index, (observed, replayed) in enumerate(zip(live, offline)):
        if canonical_json(observed) != canonical_json(replayed):
            mismatches.append({
                "stream_index": index,
                "decision_index_live": observed.get("decision_index"),
                "decision_index_offline": replayed.get("decision_index"),
            })
    if mismatches:
        raise ValueError(f"live/offline recommendation drift: {mismatches[:5]}")

    decision_indices = [row.get("decision_index") for row in live]
    capture_indices = [row.get("decision_index") for row in captures]
    if decision_indices != capture_indices:
        raise ValueError("live sidecar recommendation order differs from Forge capture order")

    if forge["return_boundary"]["returned_actions"] != forge["controller_acceptance_events"]:
        raise ValueError("Stage 8F controller acceptance no longer matches returned actions")
    if forge["failed_dispatches"] != 0:
        raise ValueError("Forge dispatch failure during Stage 8G validation")

    return {
        "source_log": log_path.name,
        "live_sidecar_file": sidecar_path.name,
        "games_completed": forge["games_completed"],
        "captured_proposals": len(captures),
        "live_recommendations": len(live),
        "live_rejections": 0,
        "live_offline_mismatches": 0,
        "ordered_capture_match": True,
        "returned_actions": forge["returned_actions"],
        "controller_acceptance_events": forge["controller_acceptance_events"],
        "successful_dispatches": forge["successful_dispatches"],
        "failed_dispatches": forge["failed_dispatches"],
        "model_id": policy.model_id,
        "sidecar_control_channel": False,
        "forge_referee": True,
        "promotion_allowed": False,
    }


def aggregate(reports, policy):
    totals = {}
    for key in (
        "games_completed",
        "captured_proposals",
        "live_recommendations",
        "live_rejections",
        "live_offline_mismatches",
        "returned_actions",
        "controller_acceptance_events",
        "successful_dispatches",
        "failed_dispatches",
    ):
        totals[key] = sum(report[key] for report in reports)

    if totals["captured_proposals"] != totals["live_recommendations"]:
        raise ValueError("aggregate live sidecar coverage is incomplete")
    if totals["returned_actions"] != totals["controller_acceptance_events"]:
        raise ValueError("aggregate controller acceptance is incomplete")
    if totals["failed_dispatches"] != 0:
        raise ValueError("aggregate Forge dispatch failure")
    if totals["live_rejections"] or totals["live_offline_mismatches"]:
        raise ValueError("aggregate sidecar validation failure")

    return {
        "schema_version": "stage8g-live-shadow-sidecar-v1",
        "mode": "shadow-only",
        "promotion_allowed": False,
        "model_id": policy.model_id,
        "forge_referee": True,
        "sidecar_control_channel": False,
        "sidecar_input": "one-way Forge stdout stream only",
        "live_offline_exact_replay": True,
        "totals": totals,
        "logs": reports,
        "limitations": [
            "The live sidecar cannot choose, mutate or execute Forge actions.",
            "The pipe can impose stdout backpressure but carries no commands into Forge.",
            "Observed validation seeds are integration evidence, not new playing-strength evidence.",
            "Controller dispatch success is not proof that a later stack object resolved.",
        ],
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("checkpoint", type=Path)
    parser.add_argument("--log", action="append", required=True, type=Path)
    parser.add_argument("--live-shadow", action="append", required=True, type=Path)
    parser.add_argument("--expected-games", type=int, default=4)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    if len(args.log) != len(args.live_shadow):
        raise ValueError("--log and --live-shadow must be supplied in matching pairs")

    policy = load_checkpoint(args.checkpoint)
    reports = [
        audit_pair(log_path, sidecar_path, args.expected_games, policy)
        for log_path, sidecar_path in zip(args.log, args.live_shadow)
    ]
    report = aggregate(reports, policy)
    args.output.write_text(json.dumps(report, sort_keys=True, indent=2) + "\n")
    print(canonical_json({
        "captured_proposals": report["totals"]["captured_proposals"],
        "live_recommendations": report["totals"]["live_recommendations"],
        "returned_actions": report["totals"]["returned_actions"],
        "successful_dispatches": report["totals"]["successful_dispatches"],
        "live_offline_exact_replay": report["live_offline_exact_replay"],
        "promotion_allowed": report["promotion_allowed"],
    }))


if __name__ == "__main__":
    main()
