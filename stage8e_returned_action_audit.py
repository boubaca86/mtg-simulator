#!/usr/bin/env python3
"""Stage 8E: bind captured search proposals to actions actually returned to Forge.

This audits only the SpellAbilityPicker return boundary. It deliberately does not
claim that a returned action resolved, completed, or produced a particular game
outcome. Forge remains the sole rules referee and action executor.
"""
from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path

from stage7_label_outcomes import label_log
from stage8d_shadow_policy import load_checkpoint, observe_capture

CAPTURE_PREFIX = "EXPERT_STAGE8_CAPTURE: "
RETURN_PREFIX = "EXPERT_STAGE8_RETURNED_ACTION: "
PASS_PREFIX = "EXPERT_STAGE8_PRIORITY_PASS: "


def _payload(line: str, prefix: str) -> dict:
    value = json.loads(line[len(prefix):])
    if not isinstance(value, dict):
        raise ValueError("audit event must be a JSON object")
    return value


def audit_lines(lines, observation_ids, source: str, policy=None):
    captures: dict[int, tuple[int, dict]] = {}
    bound: dict[int, dict] = {}
    events = []
    game = 1
    games_completed = 0
    shadow = Counter()

    for raw in lines:
        line = raw.rstrip("\n")
        if line.startswith(CAPTURE_PREFIX):
            capture = _payload(line, CAPTURE_PREFIX)
            if capture.get("schema_version") != "stage8-capture-v1":
                raise ValueError("wrong Stage 8 capture schema")
            idx = capture.get("decision_index")
            if type(idx) is not int or idx < 0 or idx in captures:
                raise ValueError("invalid or duplicate capture decision index")
            state = capture.get("public_state")
            candidates = capture.get("candidates")
            selected = capture.get("selected_action")
            if not isinstance(state, dict) or not isinstance(candidates, list) or not candidates:
                raise ValueError("capture lacks public state or candidates")
            identities = [c.get("action_identity") for c in candidates if isinstance(c, dict)]
            if len(identities) != len(candidates) or selected not in identities:
                raise ValueError("Forge proposal is absent from capture candidates")
            captures[idx] = (game, capture)
            continue

        if line.startswith(RETURN_PREFIX):
            event = _payload(line, RETURN_PREFIX)
            if (event.get("schema_version") != "stage8e-returned-action-v1"
                    or event.get("boundary") != "spell-ability-return"
                    or event.get("resolved_or_completed") is not False
                    or event.get("promotion_allowed") is not False):
                raise ValueError("invalid returned-action boundary event")
            idx = event.get("capture_decision_index")
            if type(idx) is not int or idx < 0 or idx in bound:
                raise ValueError("invalid or multiply-bound capture")
            if idx not in captures:
                raise ValueError("returned action references an unknown/future capture")
            capture_game, capture = captures[idx]
            if capture_game != game:
                raise ValueError("returned action crossed a game boundary")
            if event.get("action_identity") != capture.get("selected_action"):
                raise ValueError("returned action differs from Forge captured proposal")
            state = capture["public_state"]
            if (event.get("turn") != state.get("turn")
                    or event.get("phase") != state.get("phase")
                    or event.get("acting_player_name") != state.get("acting_player_name")):
                raise ValueError("returned action public context differs from its capture")
            bound[idx] = event
            events.append(("returned", event))
            if policy is not None:
                pred = observe_capture(policy, capture)
                shadow["bound"] += 1
                if len(capture["candidates"]) > 1:
                    shadow["rankable_bound"] += 1
                    if pred["forge_proposal_in_top_set"]:
                        shadow["rankable_in_top_set"] += 1
                if pred["status"] == "forced":
                    shadow["forced"] += 1
                elif pred["status"] == "tied":
                    shadow["tied"] += 1
                else:
                    shadow["unique"] += 1
                    if pred["forge_proposal_in_top_set"]:
                        shadow["unique_agreement"] += 1
            continue

        if line.startswith(PASS_PREFIX):
            event = _payload(line, PASS_PREFIX)
            if (event.get("schema_version") != "stage8e-priority-pass-v1"
                    or event.get("boundary") != "spell-ability-return"
                    or event.get("promotion_allowed") is not False
                    or not isinstance(event.get("reason"), str)):
                raise ValueError("invalid priority-pass boundary event")
            events.append(("pass", event))
            continue

        if line.startswith("Game Result: Game "):
            games_completed += 1
            game += 1

    if set(captures) != set(observation_ids):
        missing = sorted(set(observation_ids) - set(captures))
        extra = sorted(set(captures) - set(observation_ids))
        raise ValueError(f"Stage 7/8 capture mismatch missing={missing[:5]} extra={extra[:5]}")

    indices = [e["priority_return_index"] for _, e in events]
    if any(type(i) is not int or i < 0 for i in indices) or indices != list(range(len(indices))):
        raise ValueError("priority-return indices are not contiguous from zero")
    if not bound:
        raise ValueError("no captured action reached the SpellAbilityPicker return boundary")
    if games_completed <= 0:
        raise ValueError("no completed games in source log")

    returned = len(bound)
    passes = sum(kind == "pass" for kind, _ in events)
    unbound = len(captures) - returned
    report = {
        "source_log": source,
        "games_completed": games_completed,
        "captured_proposals": len(captures),
        "returned_actions": returned,
        "priority_passes": passes,
        "priority_returns_total": len(events),
        "bound_capture_fraction": returned / len(captures) if captures else 0.0,
        "unbound_probe_or_deferred_captures": unbound,
        "binding_mismatches": 0,
        "boundary": "SpellAbilityPicker returned action/pass to Forge",
        "resolution_verified": False,
        "promotion_allowed": False,
    }
    if policy is not None:
        report["shadow"] = {
            "model_id": policy.model_id,
            "bound_actions_scored": shadow["bound"],
            "rankable_bound_actions": shadow["rankable_bound"],
            "rankable_returned_action_in_shadow_top_set": shadow["rankable_in_top_set"],
            "rankable_top_set_agreement_fraction": (
                shadow["rankable_in_top_set"] / shadow["rankable_bound"]
                if shadow["rankable_bound"] else None
            ),
            "forced": shadow["forced"],
            "tied": shadow["tied"],
            "unique_preference": shadow["unique"],
            "unique_preference_agreement": shadow["unique_agreement"],
            "unique_preference_agreement_fraction": (
                shadow["unique_agreement"] / shadow["unique"] if shadow["unique"] else None
            ),
        }
    return report


def audit_log(path: Path, expected_games: int, policy=None):
    observations = label_log(path, expected_games=expected_games)
    ids = [row["decision_index"] for row in observations]
    report = audit_lines(path.read_text().splitlines(), ids, path.name, policy)
    if report["games_completed"] != expected_games:
        raise ValueError("game count differs from whole-log validator")
    return report


def aggregate(reports, policy=None):
    keys = (
        "games_completed", "captured_proposals", "returned_actions",
        "priority_passes", "priority_returns_total",
        "unbound_probe_or_deferred_captures", "binding_mismatches",
    )
    totals = {key: sum(r[key] for r in reports) for key in keys}
    totals["bound_capture_fraction"] = (
        totals["returned_actions"] / totals["captured_proposals"]
        if totals["captured_proposals"] else 0.0
    )
    out = {
        "schema_version": "stage8e-returned-action-audit-v1",
        "mode": "shadow-only",
        "promotion_allowed": False,
        "boundary": "SpellAbilityPicker returned action/pass to Forge",
        "resolution_verified": False,
        "totals": totals,
        "logs": reports,
        "limitations": [
            "A returned action is not yet proof that the action resolved or completed.",
            "Unbound captures are search probes or deferred plans and must not be treated as executed moves.",
            "Shadow recommendations never choose, mutate or execute Forge actions.",
            "This instrumentation run is not new playing-strength evidence.",
        ],
    }
    if policy is not None:
        s = Counter()
        for report in reports:
            sh = report["shadow"]
            for key in (
                "bound_actions_scored", "rankable_bound_actions",
                "rankable_returned_action_in_shadow_top_set", "forced", "tied",
                "unique_preference", "unique_preference_agreement",
            ):
                s[key] += sh[key]
        out["shadow"] = {
            "model_id": policy.model_id,
            **dict(s),
            "rankable_top_set_agreement_fraction": (
                s["rankable_returned_action_in_shadow_top_set"] / s["rankable_bound_actions"]
                if s["rankable_bound_actions"] else None
            ),
            "unique_preference_agreement_fraction": (
                s["unique_preference_agreement"] / s["unique_preference"]
                if s["unique_preference"] else None
            ),
        }
    return out


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("checkpoint", type=Path)
    parser.add_argument("logs", nargs="+", type=Path)
    parser.add_argument("--expected-games", type=int, default=4)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    policy = load_checkpoint(args.checkpoint)
    reports = [audit_log(path, args.expected_games, policy) for path in args.logs]
    report = aggregate(reports, policy)
    args.output.write_text(json.dumps(report, sort_keys=True, indent=2) + "\n")
    print(json.dumps({
        "returned_actions": report["totals"]["returned_actions"],
        "priority_passes": report["totals"]["priority_passes"],
        "unbound_probe_or_deferred_captures": report["totals"]["unbound_probe_or_deferred_captures"],
        "binding_mismatches": report["totals"]["binding_mismatches"],
        "rankable_top_set_agreement_fraction": report["shadow"]["rankable_top_set_agreement_fraction"],
        "promotion_allowed": report["promotion_allowed"],
    }, sort_keys=True))


if __name__ == "__main__":
    main()
