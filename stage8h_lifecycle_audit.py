#!/usr/bin/env python3
"""Stage 8H: audit every controller-accepted expert action to a Forge terminal lifecycle outcome."""
from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path

from stage8d_shadow_policy import load_checkpoint
from stage8f_acceptance_audit import (
    ACCEPT_PREFIX, audit_log as audit_acceptance_log,
)

TERMINAL_PREFIX = "EXPERT_STAGE8H_ACTION_TERMINAL: "
ALLOWED_OUTCOMES = {
    "resolved",
    "fizzled",
    "removed-before-resolution",
    "no-stack-completed",
    "dispatch-failed",
    "nonland-success-without-stack-binding",
}
ANOMALY_OUTCOMES = {
    "dispatch-failed",
    "nonland-success-without-stack-binding",
}


def _payload(line: str, prefix: str) -> dict:
    value = json.loads(line[len(prefix):])
    if not isinstance(value, dict):
        raise ValueError("Stage 8H event must be a JSON object")
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
            if type(idx) is not int or idx < 0 or idx in accepted:
                raise ValueError("invalid or duplicate Stage 8F acceptance in Stage 8H audit")
            accepted[idx] = (game, event)
            continue

        if line.startswith(TERMINAL_PREFIX):
            event = _payload(line, TERMINAL_PREFIX)
            if (event.get("schema_version") != "stage8h-action-terminal-v1"
                    or event.get("boundary") != "forge-action-terminal"
                    or event.get("promotion_allowed") is not False):
                raise ValueError("invalid Stage 8H terminal event")

            idx = event.get("capture_decision_index")
            if type(idx) is not int or idx < 0 or idx in terminals:
                raise ValueError("invalid or duplicate Stage 8H terminal binding")
            if idx not in accepted:
                raise ValueError("Stage 8H terminal references unknown/future controller acceptance")

            accept_game, accepted_event = accepted[idx]
            if accept_game != game:
                raise ValueError("Stage 8H terminal crossed a game boundary")
            if event.get("priority_return_index") != accepted_event.get("priority_return_index"):
                raise ValueError("Stage 8H priority-return index drift")
            if event.get("action_identity") != accepted_event.get("action_identity"):
                raise ValueError("Stage 8H action identity drift")
            if event.get("acting_player_name") != accepted_event.get("acting_player_name"):
                raise ValueError("Stage 8H acting-player drift")
            if (event.get("return_turn") != accepted_event.get("turn")
                    or event.get("return_phase") != accepted_event.get("phase")):
                raise ValueError("Stage 8H return context drift")

            outcome = event.get("outcome")
            if outcome not in ALLOWED_OUTCOMES:
                raise ValueError("unknown Stage 8H terminal outcome")
            if type(event.get("stack_based")) is not bool:
                raise ValueError("Stage 8H stack_based must be boolean")
            if type(event.get("terminal_turn")) is not int or event["terminal_turn"] < 0:
                raise ValueError("invalid Stage 8H terminal turn")
            if not isinstance(event.get("terminal_phase"), str):
                raise ValueError("invalid Stage 8H terminal phase")
            fizzled = event.get("fizzled")
            if fizzled is not None and type(fizzled) is not bool:
                raise ValueError("Stage 8H fizzled must be boolean or null")

            dispatch_success = accepted_event.get("dispatch_success")
            land_ability = accepted_event.get("land_ability")
            if type(dispatch_success) is not bool or type(land_ability) is not bool:
                raise ValueError("Stage 8H requires typed Stage 8F dispatch metadata")

            if outcome == "resolved":
                if dispatch_success is not True or land_ability is True:
                    raise ValueError("resolved action disagrees with controller dispatch")
                if event["stack_based"] is not True or fizzled is not False:
                    raise ValueError("resolved terminal semantics mismatch")
            elif outcome == "fizzled":
                if dispatch_success is not True or land_ability is True:
                    raise ValueError("fizzled action disagrees with controller dispatch")
                if event["stack_based"] is not True or fizzled is not True:
                    raise ValueError("fizzled terminal semantics mismatch")
            elif outcome == "removed-before-resolution":
                if dispatch_success is not True or land_ability is True:
                    raise ValueError("removed action disagrees with controller dispatch")
                if event["stack_based"] is not True or fizzled is not None:
                    raise ValueError("removed terminal semantics mismatch")
            elif outcome == "no-stack-completed":
                if dispatch_success is not True or land_ability is not True:
                    raise ValueError("no-stack action disagrees with controller dispatch")
                if event["stack_based"] is not False or fizzled is not False:
                    raise ValueError("no-stack terminal semantics mismatch")
            elif outcome == "dispatch-failed":
                if dispatch_success is not False or event["stack_based"] is not False:
                    raise ValueError("dispatch-failed terminal semantics mismatch")
            elif outcome == "nonland-success-without-stack-binding":
                if dispatch_success is not True or land_ability is True or event["stack_based"] is not False:
                    raise ValueError("unbound-success terminal semantics mismatch")

            terminals[idx] = (game, event)
            continue

        if line.startswith("Game Result: Game "):
            game += 1

    expected_acceptances = base_report["controller_acceptance_events"]
    if len(accepted) != expected_acceptances:
        raise ValueError(
            "Stage 8H controller-acceptance count differs from Stage 8F audit"
        )

    pending = sorted(set(accepted) - set(terminals))
    unexpected = sorted(set(terminals) - set(accepted))
    if unexpected:
        raise ValueError(
            f"Stage 8H terminal without controller acceptance: {unexpected[:10]}"
        )

    outcomes = Counter(event["outcome"] for _, event in terminals.values())
    anomalies = sum(outcomes[name] for name in ANOMALY_OUTCOMES)
    return {
        "source_log": source,
        "games_completed": base_report["games_completed"],
        "returned_actions": base_report["returned_actions"],
        "controller_acceptance_events": len(accepted),
        "successful_dispatches": base_report["successful_dispatches"],
        "failed_dispatches": base_report["failed_dispatches"],
        "terminal_events": len(terminals),
        "terminal_coverage_fraction": (
            len(terminals) / len(accepted) if accepted else None
        ),
        "pending_at_game_end": len(pending),
        "pending_capture_decision_indices": pending,
        "terminal_outcome_counts": dict(sorted(outcomes.items())),
        "terminal_binding_mismatches": 0,
        "lifecycle_anomalies": anomalies,
        "promotion_allowed": False,
        "boundary": "controller acceptance through exact Forge action terminal",
        "limitations": [
            "Terminal binding proves Forge lifecycle handling, not that the learned shadow policy chose the action.",
            "This integration corpus uses an already-observed seed and is not fresh playing-strength evidence.",
        ],
    }


def audit_log(path: Path, expected_games: int, policy=None):
    # Existing whole-log validation runs first and quarantines timeouts,
    # exceptions, replay failures or strategy-fusion failures.
    base = audit_acceptance_log(path, expected_games, policy)
    report = audit_lifecycle_lines(path.read_text().splitlines(), path.name, base)
    report["acceptance_boundary"] = base
    return report


def aggregate(reports):
    sum_keys = (
        "games_completed",
        "returned_actions",
        "controller_acceptance_events",
        "successful_dispatches",
        "failed_dispatches",
        "terminal_events",
        "pending_at_game_end",
        "terminal_binding_mismatches",
        "lifecycle_anomalies",
    )
    totals = {key: sum(r[key] for r in reports) for key in sum_keys}
    totals["terminal_coverage_fraction"] = (
        totals["terminal_events"] / totals["controller_acceptance_events"]
        if totals["controller_acceptance_events"] else None
    )

    outcomes = Counter()
    for report in reports:
        outcomes.update(report["terminal_outcome_counts"])

    # Preserve the frozen Stage 8D model's already-computed returned-action
    # agreement metrics. Terminal outcomes are never model inputs.
    shadow = None
    model_ids = set()
    shadow_sums = Counter()
    for report in reports:
        sh = report["acceptance_boundary"].get("return_boundary", {}).get("shadow")
        if not sh:
            continue
        model_ids.add(sh["model_id"])
        for key in (
            "bound_actions_scored",
            "rankable_bound_actions",
            "rankable_returned_action_in_shadow_top_set",
            "forced",
            "tied",
            "unique_preference",
            "unique_preference_agreement",
        ):
            shadow_sums[key] += sh[key]

    if shadow_sums:
        if len(model_ids) != 1:
            raise ValueError("Stage 8H source logs used different shadow models")
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
        "schema_version": "stage8h-terminal-lifecycle-audit-v1",
        "mode": "shadow-only",
        "promotion_allowed": False,
        "forge_referee": True,
        "boundary": "controller acceptance through exact Forge action terminal",
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
        "returned_actions": report["totals"]["returned_actions"],
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
