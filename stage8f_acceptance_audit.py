#!/usr/bin/env python3
"""Stage 8F: verify that Stage 8E returned actions reach Forge's real AI controller dispatch.

This stage remains observation-only. It does not alter Forge's controller return
value, card rules, legal actions, targets, costs, modes or choices.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from stage8e_returned_action_audit import (
    RETURN_PREFIX, audit_log as audit_return_log, aggregate as aggregate_returns,
)
from stage8d_shadow_policy import load_checkpoint

ACCEPT_PREFIX = "EXPERT_STAGE8_CONTROLLER_ACCEPTANCE: "


def _payload(line: str, prefix: str) -> dict:
    value = json.loads(line[len(prefix):])
    if not isinstance(value, dict):
        raise ValueError("Stage 8F event must be a JSON object")
    return value


def audit_acceptance_lines(lines, source: str, base_report: dict):
    returned: dict[int, tuple[int, dict]] = {}
    accepted: dict[int, tuple[int, dict]] = {}
    game = 1

    for raw in lines:
        line = raw.rstrip("\n")
        if line.startswith(RETURN_PREFIX):
            event = _payload(line, RETURN_PREFIX)
            idx = event.get("capture_decision_index")
            if type(idx) is not int or idx < 0 or idx in returned:
                raise ValueError("invalid or duplicate returned action in Stage 8F audit")
            returned[idx] = (game, event)
            continue

        if line.startswith(ACCEPT_PREFIX):
            event = _payload(line, ACCEPT_PREFIX)
            if (event.get("schema_version") != "stage8f-controller-acceptance-v1"
                    or event.get("boundary") != "player-controller-dispatch"
                    or event.get("resolved_or_completed") is not False
                    or event.get("promotion_allowed") is not False):
                raise ValueError("invalid Stage 8F controller-acceptance event")
            idx = event.get("capture_decision_index")
            if type(idx) is not int or idx < 0 or idx in accepted:
                raise ValueError("invalid or duplicate Stage 8F acceptance binding")
            if idx not in returned:
                raise ValueError("Stage 8F acceptance references unknown/future returned action")
            return_game, ret = returned[idx]
            if return_game != game:
                raise ValueError("Stage 8F acceptance crossed a game boundary")
            if event.get("priority_return_index") != ret.get("priority_return_index"):
                raise ValueError("Stage 8F priority-return index drift")
            if event.get("action_identity") != ret.get("action_identity"):
                raise ValueError("Stage 8F action identity drift")
            for key in ("turn", "phase", "acting_player_name"):
                if event.get(key) != ret.get(key):
                    raise ValueError(f"Stage 8F public context drift: {key}")
            if event.get("controller_return_value") is not True:
                raise ValueError("Stage 8F changed Forge controller return semantics")
            if event.get("return_context_matches") is not True:
                raise ValueError("Stage 8F bridge observed context mismatch")
            if type(event.get("dispatch_success")) is not bool:
                raise ValueError("Stage 8F dispatch success must be boolean")
            if type(event.get("land_ability")) is not bool or type(event.get("skip_after_dispatch")) is not bool:
                raise ValueError("Stage 8F dispatch flags must be boolean")
            accepted[idx] = (game, event)
            continue

        if line.startswith("Game Result: Game "):
            game += 1

    expected_returned = base_report["returned_actions"]
    if len(returned) != expected_returned:
        raise ValueError("Stage 8F returned-action count differs from Stage 8E audit")

    missing = sorted(set(returned) - set(accepted))
    if missing:
        raise ValueError(f"Stage 8F returned actions missing controller acceptance: {missing[:10]}")
    extra = sorted(set(accepted) - set(returned))
    if extra:
        raise ValueError(f"Stage 8F acceptance without returned action: {extra[:10]}")

    failures = []
    for idx, (_, event) in accepted.items():
        if not event["dispatch_success"]:
            failures.append({
                "capture_decision_index": idx,
                "priority_return_index": event["priority_return_index"],
                "action_identity": event["action_identity"],
                "land_ability": event["land_ability"],
                "skip_after_dispatch": event["skip_after_dispatch"],
            })

    return {
        "source_log": source,
        "games_completed": base_report["games_completed"],
        "returned_actions": len(returned),
        "controller_acceptance_events": len(accepted),
        "successful_dispatches": len(accepted) - len(failures),
        "failed_dispatches": len(failures),
        "dispatch_success_fraction": (
            (len(accepted) - len(failures)) / len(accepted) if accepted else None
        ),
        "missing_acceptance_events": 0,
        "binding_mismatches": 0,
        "failed_dispatch_details": failures,
        "controller_return_semantics_preserved": True,
        "boundary": "PlayerControllerAi dispatch after SpellAbilityPicker return",
        "resolution_verified": False,
        "promotion_allowed": False,
    }


def audit_log(path: Path, expected_games: int, policy=None):
    base = audit_return_log(path, expected_games, policy)
    report = audit_acceptance_lines(path.read_text().splitlines(), path.name, base)
    report["return_boundary"] = base
    return report


def aggregate(reports):
    totals = {
        "games_completed": sum(r["games_completed"] for r in reports),
        "returned_actions": sum(r["returned_actions"] for r in reports),
        "controller_acceptance_events": sum(r["controller_acceptance_events"] for r in reports),
        "successful_dispatches": sum(r["successful_dispatches"] for r in reports),
        "failed_dispatches": sum(r["failed_dispatches"] for r in reports),
        "missing_acceptance_events": sum(r["missing_acceptance_events"] for r in reports),
        "binding_mismatches": sum(r["binding_mismatches"] for r in reports),
    }
    totals["dispatch_success_fraction"] = (
        totals["successful_dispatches"] / totals["controller_acceptance_events"]
        if totals["controller_acceptance_events"] else None
    )
    shadow_reports = [r["return_boundary"] for r in reports]
    shadow = aggregate_returns(shadow_reports)
    return {
        "schema_version": "stage8f-controller-acceptance-audit-v1",
        "mode": "shadow-only",
        "promotion_allowed": False,
        "boundary": "PlayerControllerAi dispatch after SpellAbilityPicker return",
        "resolution_verified": False,
        "controller_return_semantics_preserved": all(
            r["controller_return_semantics_preserved"] for r in reports
        ),
        "totals": totals,
        "logs": reports,
        "shadow": shadow.get("shadow"),
        "limitations": [
            "Controller dispatch success is not proof that a stack object later resolved.",
            "The learned shadow policy still cannot choose or execute Forge actions.",
            "This uses observed seeds for integration validation, not new playing-strength evidence.",
            "Forge 2.0.15 remains the rules referee and existing controller return semantics are unchanged.",
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
        "controller_acceptance_events": report["totals"]["controller_acceptance_events"],
        "successful_dispatches": report["totals"]["successful_dispatches"],
        "failed_dispatches": report["totals"]["failed_dispatches"],
        "dispatch_success_fraction": report["totals"]["dispatch_success_fraction"],
        "promotion_allowed": report["promotion_allowed"],
    }, sort_keys=True))


if __name__ == "__main__":
    main()
