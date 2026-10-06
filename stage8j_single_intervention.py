#!/usr/bin/env python3
"""Stage 8J: one public-only learned action intervention per game.

The frozen Stage 8D model is allowed to request at most one action that differs
from Forge, only at a baseline action that actually reached the return boundary.
The request must still match a complete legal candidate in the live Forge state.
All later decisions remain Forge-controlled.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import re
from pathlib import Path

from stage8d_shadow_policy import CAPTURE_PREFIX, load_checkpoint, observe_capture
from stage8e_returned_action_audit import RETURN_PREFIX
from stage8f_acceptance_audit import ACCEPT_PREFIX
from stage8h_lifecycle_audit import TERMINAL_PREFIX, audit_log as audit_lifecycle_log
from stage8i_control_replay import CONTROL_PREFIX

WIN_RE = re.compile(r"^Game Result: Game 1 ended in \d+ ms\. (.+) has won!$")


def _payload(line: str, prefix: str) -> dict:
    value = json.loads(line[len(prefix):])
    if not isinstance(value, dict):
        raise ValueError("Stage 8J event must be a JSON object")
    return value


def _events(path: Path, prefix: str) -> list[dict]:
    return [
        _payload(line, prefix)
        for line in path.read_text().splitlines()
        if line.startswith(prefix)
    ]


def _winner(path: Path) -> str | None:
    results = [line for line in path.read_text().splitlines()
               if line.startswith("Game Result: Game ")]
    if len(results) != 1:
        raise ValueError("Stage 8J requires exactly one completed game per JVM")
    match = WIN_RE.match(results[0])
    if match:
        return match.group(1)
    lower = results[0].lower()
    if "draw" in lower:
        return None
    raise ValueError("unrecognized Stage 8J game result")


def _side_score(winner: str | None, actor_prefix: str) -> float:
    if winner is None:
        return 0.5
    return 1.0 if winner.startswith(actor_prefix) else 0.0


def _safe_signature(capture: dict) -> str:
    state = dict(capture["public_state"])
    # Selected-action metadata changes when the adapter replaces Forge's choice;
    # it is not part of the game-state equality check.
    state.pop("complete_action_identity", None)
    candidates = []
    for candidate in capture["candidates"]:
        candidates.append({
            "action_identity": candidate["action_identity"],
            "replay_valid_count": candidate["replay_valid_count"],
            "target_semantics_version": candidate["target_semantics_version"],
            "target_public_semantics": candidate["target_public_semantics"],
        })
    value = {
        "decision_index": capture["decision_index"],
        "run_seed": capture["run_seed"],
        "matchup_id": capture["matchup_id"],
        "public_state": state,
        "candidates": candidates,
        "information_set_samples": capture["information_set_samples"],
    }
    raw = json.dumps(value, sort_keys=True, separators=(",", ":"), allow_nan=False).encode()
    return hashlib.sha256(raw).hexdigest()


def _write_request(path: Path, decision_index: int, identity: str) -> str:
    encoded = base64.urlsafe_b64encode(identity.encode()).decode()
    raw = f"{decision_index}\t{encoded}\n".encode()
    path.write_bytes(raw)
    return hashlib.sha256(raw).hexdigest()


def plan_intervention(checkpoint: Path, baseline: Path, output: Path,
                      metadata: Path, actor_prefix: str) -> dict:
    policy = load_checkpoint(checkpoint)
    audit = audit_lifecycle_log(baseline, 1, policy)
    if audit["lifecycle_anomalies"] or audit["pending_at_game_end"]:
        raise ValueError("baseline lifecycle is not clean")

    returned = {
        event["capture_decision_index"]
        for event in _events(baseline, RETURN_PREFIX)
    }
    captures = _events(baseline, CAPTURE_PREFIX)
    if not captures:
        raise ValueError("baseline contains no Stage 8 captures")

    chosen = None
    for capture in captures:
        idx = capture["decision_index"]
        state = capture["public_state"]
        if idx not in returned:
            continue
        if not str(state.get("acting_player_name", "")).startswith(actor_prefix):
            continue
        prediction = observe_capture(policy, capture)
        if prediction["status"] != "ranked" or prediction["recommendation"] is None:
            continue
        if prediction["recommendation"] == capture["selected_action"]:
            continue

        scores = sorted(
            (float(row["model_score"]) for row in prediction["candidate_scores"]),
            reverse=True,
        )
        margin = scores[0] - scores[1] if len(scores) > 1 else None
        chosen = {
            "decision_index": idx,
            "actor": state["acting_player_name"],
            "turn": state["turn"],
            "phase": state["phase"],
            "forge_action": capture["selected_action"],
            "learned_action": prediction["recommendation"],
            "model_score_margin": margin,
            "candidate_count": len(capture["candidates"]),
            "safe_capture_sha256": _safe_signature(capture),
        }
        break

    if chosen is None:
        output.write_bytes(b"")
        request_sha = hashlib.sha256(b"").hexdigest()
    else:
        request_sha = _write_request(
            output, chosen["decision_index"], chosen["learned_action"]
        )

    report = {
        "schema_version": "stage8j-single-intervention-plan-v1",
        "mode": "single-public-only-learned-intervention",
        "model_id": policy.model_id,
        "actor_prefix": actor_prefix,
        "baseline_log": baseline.name,
        "intervention_planned": chosen is not None,
        "intervention": chosen,
        "request_file": output.name,
        "request_file_sha256": request_sha,
        "request_fields": ["decision_index", "complete_action_identity"],
        "hidden_information_fields": 0,
        "forge_referee": True,
        "promotion_allowed": False,
    }
    metadata.write_text(json.dumps(report, sort_keys=True, indent=2) + "\n")
    return report


def compare_pair(checkpoint, baseline: Path, controlled: Path, plan_path: Path,
                 request_path: Path, actor_prefix: str) -> dict:
    base_audit = audit_lifecycle_log(baseline, 1, checkpoint)
    ctrl_audit = audit_lifecycle_log(controlled, 1, checkpoint)
    for name, audit in (("baseline", base_audit), ("controlled", ctrl_audit)):
        if (audit["lifecycle_anomalies"] != 0
                or audit["pending_at_game_end"] != 0
                or audit["terminal_coverage_fraction"] != 1.0
                or audit["failed_dispatches"] != 0):
            raise ValueError(f"Stage 8J {name} lifecycle is not clean")

    plan = json.loads(plan_path.read_text())
    if plan.get("schema_version") != "stage8j-single-intervention-plan-v1":
        raise ValueError("wrong Stage 8J plan schema")
    if plan.get("actor_prefix") != actor_prefix or plan.get("forge_referee") is not True:
        raise ValueError("Stage 8J plan actor/referee drift")
    if plan.get("promotion_allowed") is not False:
        raise ValueError("Stage 8J plan unexpectedly allows promotion")

    base_caps = _events(baseline, CAPTURE_PREFIX)
    ctrl_caps = _events(controlled, CAPTURE_PREFIX)
    controls = _events(controlled, CONTROL_PREFIX)
    intervention = plan.get("intervention")

    applied = False
    intervention_terminal = None
    if plan["intervention_planned"]:
        if not request_path.read_bytes():
            raise ValueError("planned Stage 8J intervention has empty request file")
        if len(controls) != 1:
            raise ValueError("Stage 8J requires exactly one applied control request")
        event = controls[0]
        idx = intervention["decision_index"]
        if (event.get("decision_index") != idx
                or event.get("acting_player_name") != intervention["actor"]
                or event.get("requested_action") != intervention["learned_action"]
                or event.get("forge_selected_action") != intervention["forge_action"]
                or event.get("requested_action_in_candidates") is not True
                or event.get("same_as_forge") is not False
                or event.get("forge_referee") is not True
                or event.get("promotion_allowed") is not False):
            raise ValueError("Stage 8J control event does not match predeclared intervention")

        base_by_idx = {row["decision_index"]: row for row in base_caps}
        ctrl_by_idx = {row["decision_index"]: row for row in ctrl_caps}
        if idx not in base_by_idx or idx not in ctrl_by_idx:
            raise ValueError("Stage 8J intervention capture is missing")
        for before_idx in range(idx):
            if base_by_idx.get(before_idx) != ctrl_by_idx.get(before_idx):
                raise ValueError("Stage 8J game drifted before the planned intervention")

        base_capture = base_by_idx[idx]
        ctrl_capture = ctrl_by_idx[idx]
        if _safe_signature(base_capture) != intervention["safe_capture_sha256"]:
            raise ValueError("Stage 8J baseline intervention state changed after planning")
        if _safe_signature(ctrl_capture) != intervention["safe_capture_sha256"]:
            raise ValueError("Stage 8J live intervention state/candidates differ from baseline")
        if base_capture["selected_action"] != intervention["forge_action"]:
            raise ValueError("Stage 8J baseline Forge action drift")
        if ctrl_capture["selected_action"] != intervention["learned_action"]:
            raise ValueError("Stage 8J controlled capture did not install learned action")

        returned = {e["capture_decision_index"]: e for e in _events(controlled, RETURN_PREFIX)}
        accepted = {e["capture_decision_index"]: e for e in _events(controlled, ACCEPT_PREFIX)}
        terminals = {e["capture_decision_index"]: e for e in _events(controlled, TERMINAL_PREFIX)}
        if idx not in returned or idx not in accepted or idx not in terminals:
            raise ValueError("Stage 8J learned intervention did not reach full Forge lifecycle")
        intervention_terminal = terminals[idx]["outcome"]
        if intervention_terminal in {
            "dispatch-failed", "nonland-success-without-stack-binding"
        }:
            raise ValueError("Stage 8J learned intervention hit a lifecycle anomaly")
        applied = True
    else:
        if request_path.read_bytes() or controls:
            raise ValueError("Stage 8J no-intervention plan unexpectedly controlled gameplay")

    baseline_winner = _winner(baseline)
    controlled_winner = _winner(controlled)
    baseline_score = _side_score(baseline_winner, actor_prefix)
    controlled_score = _side_score(controlled_winner, actor_prefix)

    return {
        "baseline_log": baseline.name,
        "controlled_log": controlled.name,
        "plan_file": plan_path.name,
        "request_file": request_path.name,
        "intervention_planned": plan["intervention_planned"],
        "intervention_applied": applied,
        "intervention_terminal_outcome": intervention_terminal,
        "baseline_winner": baseline_winner,
        "controlled_winner": controlled_winner,
        "controlled_side_baseline_score": baseline_score,
        "controlled_side_result_score": controlled_score,
        "controlled_side_score_delta": controlled_score - baseline_score,
        "pre_intervention_drift": 0,
        "invalid_requests_accepted": 0,
        "lifecycle_anomalies": 0,
        "forge_referee": True,
        "promotion_allowed": False,
    }


def aggregate(pairs: list[dict], minimum_interventions: int) -> dict:
    planned = sum(p["intervention_planned"] for p in pairs)
    applied = sum(p["intervention_applied"] for p in pairs)
    baseline_score = sum(p["controlled_side_baseline_score"] for p in pairs)
    controlled_score = sum(p["controlled_side_result_score"] for p in pairs)
    anomalies = sum(p["lifecycle_anomalies"] for p in pairs)
    invalid = sum(p["invalid_requests_accepted"] for p in pairs)
    drift = sum(p["pre_intervention_drift"] for p in pairs)
    return {
        "schema_version": "stage8j-single-intervention-result-v1",
        "mode": "single-public-only-learned-intervention",
        "promotion_allowed": False,
        "broader_learned_control_allowed": False,
        "forge_referee": True,
        "minimum_interventions": minimum_interventions,
        "pairs": pairs,
        "totals": {
            "games": len(pairs),
            "interventions_planned": planned,
            "interventions_applied": applied,
            "lifecycle_anomalies": anomalies,
            "invalid_requests_accepted": invalid,
            "pre_intervention_drift": drift,
            "controlled_side_baseline_score": baseline_score,
            "controlled_side_result_score": controlled_score,
            "controlled_side_score_delta": controlled_score - baseline_score,
        },
        "safety_gate_passed": (
            len(pairs) > 0
            and planned == applied
            and applied >= minimum_interventions
            and anomalies == 0
            and invalid == 0
            and drift == 0
        ),
        "interpretation": (
            "This gate evaluates bounded live-control safety only. The frozen model "
            "was trained to approximate Forge fixed-root scores, so outcome deltas "
            "are descriptive and are not a promotion criterion."
        ),
        "next_stage": (
            "Collect outcome-bearing exploration trajectories and train/evaluate an "
            "outcome-oriented policy/value signal before broader learned control."
        ),
    }


def main():
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)

    plan = sub.add_parser("plan")
    plan.add_argument("checkpoint", type=Path)
    plan.add_argument("baseline", type=Path)
    plan.add_argument("--actor-prefix", default="Ai(1)-")
    plan.add_argument("--output", required=True, type=Path)
    plan.add_argument("--metadata", required=True, type=Path)

    compare = sub.add_parser("compare")
    compare.add_argument("checkpoint", type=Path)
    compare.add_argument("--baseline", nargs="+", required=True, type=Path)
    compare.add_argument("--controlled", nargs="+", required=True, type=Path)
    compare.add_argument("--plans", nargs="+", required=True, type=Path)
    compare.add_argument("--requests", nargs="+", required=True, type=Path)
    compare.add_argument("--actor-prefix", default="Ai(1)-")
    compare.add_argument("--minimum-interventions", type=int, default=4)
    compare.add_argument("--output", required=True, type=Path)

    args = parser.parse_args()
    if args.command == "plan":
        result = plan_intervention(
            args.checkpoint, args.baseline, args.output, args.metadata,
            args.actor_prefix,
        )
        print(json.dumps({
            "intervention_planned": result["intervention_planned"],
            "intervention": result["intervention"],
            "model_id": result["model_id"],
        }, sort_keys=True))
        return

    lengths = {
        len(args.baseline), len(args.controlled), len(args.plans), len(args.requests)
    }
    if len(lengths) != 1:
        raise ValueError("Stage 8J pair input lists must have equal lengths")
    policy = load_checkpoint(args.checkpoint)
    pairs = [
        compare_pair(policy, b, c, p, r, args.actor_prefix)
        for b, c, p, r in zip(
            args.baseline, args.controlled, args.plans, args.requests
        )
    ]
    report = aggregate(pairs, args.minimum_interventions)
    args.output.write_text(json.dumps(report, sort_keys=True, indent=2) + "\n")
    print(json.dumps({
        "safety_gate_passed": report["safety_gate_passed"],
        **report["totals"],
        "promotion_allowed": report["promotion_allowed"],
    }, sort_keys=True))


if __name__ == "__main__":
    main()
