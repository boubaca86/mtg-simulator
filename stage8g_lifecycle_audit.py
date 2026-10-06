#!/usr/bin/env python3
"""Stage 8G: audit accepted expert-AI actions through Forge terminal lifecycle events."""
from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path

from stage8d_shadow_policy import load_checkpoint
from stage8f_acceptance_audit import (
    ACCEPT_PREFIX, audit_log as audit_acceptance_log,
)

TERMINAL_PREFIX = "EXPERT_STAGE8_ACTION_TERMINAL: "
ALLOWED_OUTCOMES = {
    "resolved",
    "fizzled",
    "removed-before-resolution",
    "no-stack-completed",
    "dispatch-failed",
    "nonland-success-without-stack-binding",
}


def _payload(line: str, prefix: str) -> dict:
    value = json.loads(line[len(prefix):])
    if not isinstance(value, dict):
        raise ValueError("Stage 8G event must be a JSON object")
    return value


def audit_lifecycle_lines(lines, source: str, base_report: dict):
    accepted: dict[int, tuple[int, dict]] = {}
    terminals: dict[int, tuple[int, dict]] = {}
    game = 1

    for raw in lines:
        line = raw.rstrip("\n")
        if line.startswith(ACCEPT_PREFIX):
            event = _payload(line, ACCEPT_PREFIX)
            idx = event.get("capture_decision_index")
            if event.get("dispatch_success") is True:
                if type(idx) is not int or idx < 0 or idx in accepted:
                    raise ValueError("invalid or duplicate successful Stage 8F acceptance")
                accepted[idx] = (game, event)
            continue

        if line.startswith(TERMINAL_PREFIX):
            event = _payload(line, TERMINAL_PREFIX)
            if (event.get("schema_version") != "stage8g-action-terminal-v1"
                    or event.get("boundary") != "forge-action-terminal"
                    or event.get("promotion_allowed") is not False):
                raise ValueError("invalid Stage 8G terminal event")
            idx = event.get("capture_decision_index")
            if type(idx) is not int or idx < 0 or idx in terminals:
                raise ValueError("invalid or duplicate Stage 8G terminal binding")
            if idx not in accepted:
                raise ValueError("Stage 8G terminal references unknown/future successful acceptance")
            accept_game, accepted_event = accepted[idx]
            if accept_game != game:
                raise ValueError("Stage 8G terminal crossed a game boundary")
            if event.get("priority_return_index") != accepted_event.get("priority_return_index"):
                raise ValueError("Stage 8G priority-return index drift")
            if event.get("action_identity") != accepted_event.get("action_identity"):
                raise ValueError("Stage 8G action identity drift")
            if event.get("acting_player_name") != accepted_event.get("acting_player_name"):
                raise ValueError("Stage 8G acting-player drift")
            if (event.get("return_turn") != accepted_event.get("turn")
                    or event.get("return_phase") != accepted_event.get("phase")):
                raise ValueError("Stage 8G return context drift")

            outcome = event.get("outcome")
            if outcome not in ALLOWED_OUTCOMES:
                raise ValueError("unknown Stage 8G terminal outcome")
            if type(event.get("stack_based")) is not bool:
                raise ValueError("Stage 8G stack_based must be boolean")
            if type(event.get("terminal_turn")) is not int or event["terminal_turn"] < 0:
                raise ValueError("invalid Stage 8G terminal turn")
            if not isinstance(event.get("terminal_phase"), str):
                raise ValueError("invalid Stage 8G terminal phase")
            fizzled = event.get("fizzled")
            if fizzled is not None and type(fizzled) is not bool:
                raise ValueError("Stage 8G fizzled must be boolean or null")

            if outcome == "resolved":
                if event["stack_based"] is not True or fizzled is not False:
                    raise ValueError("resolved terminal semantics mismatch")
            elif outcome == "fizzled":
                if event["stack_based"] is not True or fizzled is not True:
                    raise ValueError("fizzled terminal semantics mismatch")
            elif outcome == "removed-before-resolution":
                if event["stack_based"] is not True or fizzled is not None:
                    raise ValueError("removed terminal semantics mismatch")
            elif outcome == "no-stack-completed":
                if event["stack_based"] is not False or fizzled is not False:
                    raise ValueError("no-stack terminal semantics mismatch")
            elif outcome in ("dispatch-failed", "nonland-success-without-stack-binding"):
                if event["stack_based"] is not False:
                    raise ValueError("dispatch anomaly terminal semantics mismatch")

            terminals[idx] = (game, event)
            continue

        if line.startswith("Game Result: Game "):
            game += 1

    expected_success = base_report["successful_dispatches"]
    if len(accepted) != expected_success:
        raise ValueError("Stage 8G successful acceptance count differs from Stage 8F audit")

    pending = sorted(set(accepted) - set(terminals))
    unexpected = sorted(set(terminals) - set(accepted))
    if unexpected:
        raise ValueError(f"Stage 8G terminal without successful acceptance: {unexpected[:10]}")

    outcomes = Counter(event["outcome"] for _, event in terminals.values())
    anomalies = outcomes["dispatch-failed"] + outcomes["nonland-success-without-stack-binding"]
    return {
        "source_log": source,
        "games_completed": base_report["games_completed"],
        "successful_dispatches": len(accepted),
        "terminal_events": len(terminals),
        "terminal_coverage_fraction": len(terminals) / len(accepted) if accepted else None,
        "pending_at_game_end": len(pending),
        "pending_capture_decision_indices": pending,
        "terminal_outcome_counts": dict(sorted(outcomes.items())),
        "terminal_binding_mismatches": 0,
        "lifecycle_anomalies": anomalies,
        "promotion_allowed": False,
        "boundary": "accepted action through Forge terminal lifecycle",
        "limitations": [
            "A clean terminal binding proves Forge lifecycle completion, not that the learned shadow policy chose the action.",
            "This integration corpus uses an already-observed seed and is not fresh playing-strength evidence.",
        ],
    }


def audit_log(path: Path, expected_games: int, policy=None):
    base = audit_acceptance_log(path, expected_games, policy)
    report = audit_lifecycle_lines(path.read_text().splitlines(), path.name, base)
    report["acceptance_boundary"] = base
    return report


def aggregate(reports):
    totals = {
        "games_completed": sum(r["games_completed"] for r in reports),
        "successful_dispatches": sum(r["successful_dispatches"] for r in reports),
        "terminal_events": sum(r["terminal_events"] for r in reports),
        "pending_at_game_end": sum(r["pending_at_game_end"] for r in reports),
        "terminal_binding_mismatches": sum(r["terminal_binding_mismatches"] for r in reports),
        "lifecycle_anomalies": sum(r["lifecycle_anomalies"] for r in reports),
    }
    totals["terminal_coverage_fraction"] = (
        totals["terminal_events"] / totals["successful_dispatches"]
        if totals["successful_dispatches"] else None
    )
    outcomes = Counter()
    for report in reports:
        outcomes.update(report["terminal_outcome_counts"])

    # The frozen shadow metrics are already computed by Stage 8E underneath the
    # Stage 8F report. Preserve them without recomputing from terminal outcomes.
    shadow = None
    model_ids = set()
    shadow_sums = Counter()
    for report in reports:
        sh = report["acceptance_boundary"].get("return_boundary", {}).get("shadow")
        if sh:
            model_ids.add(sh["model_id"])
            for key in (
                "bound_actions_scored", "rankable_bound_actions",
                "rankable_returned_action_in_shadow_top_set", "forced", "tied",
                "unique_preference", "unique_preference_agreement",
            ):
                shadow_sums[key] += sh[key]
    if shadow_sums:
        if len(model_ids) != 1:
            raise ValueError("Stage 8G source logs used different shadow models")
        shadow = {
            "model_id": next(iter(model_ids)),
            **dict(shadow_sums),
            "rankable_top_set_agreement_fraction": (
                shadow_sums["rankable_returned_action_in_shadow_top_set"]
                / shadow_sums["rankable_bound_actions"]
                if shadow_sums["rankable_bound_actions"] else None
            ),
            "unique_preference_agreement_fraction": (
                shadow_sums["unique_preference_agreement"]
                / shadow_sums["unique_preference"]
                if shadow_sums["unique_preference"] else None
            ),
        }

    return {
        "schema_version": "stage8g-terminal-lifecycle-audit-v1",
        "mode": "shadow-only",
        "promotion_allowed": False,
        "boundary": "accepted action through Forge terminal lifecycle",
        "totals": totals,
        "terminal_outcome_counts": dict(sorted(outcomes.items())),
        "shadow": shadow,
        "logs": reports,
        "limitations": [
            "The learned model remains read-only and cannot select or execute Forge actions.",
            "Forge 2.0.15 remains the rules referee.",
            "This integration validation reuses observed seeds and is not a win-rate promotion test.",
        ],
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("checkpoint", type=Path)
    parser.add_argument("logs", nargs="+", type=Path)
    parser.add_argument("--expected-games", type=int, default=4)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    policy = load_checkpoint(args.checkpoint)
    reports = [audit_log(path, args.expected_games, policy) for path in args.logs]
    report = aggregate(reports)
    args.output.write_text(json.dumps(report, sort_keys=True, indent=2) + "\n")
    print(json.dumps({
        "successful_dispatches": report["totals"]["successful_dispatches"],
        "terminal_events": report["totals"]["terminal_events"],
        "pending_at_game_end": report["totals"]["pending_at_game_end"],
        "terminal_coverage_fraction": report["totals"]["terminal_coverage_fraction"],
        "lifecycle_anomalies": report["totals"]["lifecycle_anomalies"],
        "terminal_outcomes": report["terminal_outcome_counts"],
        "promotion_allowed": report["promotion_allowed"],
    }, sort_keys=True))


if __name__ == "__main__":
    main()
