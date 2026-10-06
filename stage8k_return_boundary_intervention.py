#!/usr/bin/env python3
"""Stage 8K: apply one learned action only after Forge commits to returning an action.

This stage corrects the Stage 8J control-boundary failure. Forge performs its
normal search and current-vs-later-phase decision first. A learned replacement
may then substitute exactly one already-captured legal complete action at the
post-plan/pre-return boundary. Forge remains the legality/rules referee.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import re
from pathlib import Path

from stage7_label_outcomes import label_log
from stage8d_shadow_policy import CAPTURE_PREFIX, load_checkpoint, observe_capture
from stage8e_returned_action_audit import RETURN_PREFIX, PASS_PREFIX
from stage8f_acceptance_audit import ACCEPT_PREFIX
from stage8h_lifecycle_audit import (
    TERMINAL_PREFIX,
    ALLOWED_OUTCOMES,
    ANOMALY_OUTCOMES,
    audit_log as audit_lifecycle_log,
)

ARM_PREFIX = "EXPERT_STAGE8K_CONTROL_ARMED: "
CONTROL_PREFIX = "EXPERT_STAGE8K_RETURN_BOUNDARY_SELECTION: "
WIN_RE = re.compile(r"^Game Result: Game 1 ended in \\d+ ms\\. (.+) has won!$")


def _payload(line: str, prefix: str) -> dict:
    value = json.loads(line[len(prefix):])
    if not isinstance(value, dict):
        raise ValueError("Stage 8K event must be a JSON object")
    return value


def _events(path: Path, prefix: str) -> list[dict]:
    return [
        _payload(line, prefix)
        for line in path.read_text().splitlines()
        if line.startswith(prefix)
    ]


def _winner(path: Path) -> str | None:
    results = [
        line for line in path.read_text().splitlines()
        if line.startswith("Game Result: Game ")
    ]
    if len(results) != 1:
        raise ValueError("Stage 8K requires exactly one completed game per JVM")
    match = WIN_RE.match(results[0])
    if match:
        return match.group(1)
    if "draw" in results[0].lower():
        return None
    raise ValueError("unrecognized Stage 8K game result")


def _side_score(winner: str | None, actor_prefix: str) -> float:
    if winner is None:
        return 0.5
    return 1.0 if winner.startswith(actor_prefix) else 0.0


def _target_field(identity: str) -> str:
    fields = [part for part in identity.split("|") if part.startswith("targets=")]
    if len(fields) != 1:
        raise ValueError("complete action identity lacks exactly one targets field")
    return fields[0][len("targets="):]


def _is_targeted(identity: str) -> bool:
    return _target_field(identity) != "<none>"


def _safe_signature(capture: dict) -> str:
    state = dict(capture["public_state"])
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
    raw = json.dumps(
        value, sort_keys=True, separators=(",", ":"), allow_nan=False
    ).encode()
    return hashlib.sha256(raw).hexdigest()


def _write_request(
    path: Path, decision_index: int, forge_identity: str, learned_identity: str
) -> str:
    forge_encoded = base64.urlsafe_b64encode(forge_identity.encode()).decode()
    learned_encoded = base64.urlsafe_b64encode(learned_identity.encode()).decode()
    raw = f"{decision_index}\\t{forge_encoded}\\t{learned_encoded}\\n".encode()
    path.write_bytes(raw)
    return hashlib.sha256(raw).hexdigest()


def plan_intervention(
    checkpoint: Path,
    baseline: Path,
    output: Path,
    metadata: Path,
    actor_prefix: str,
) -> dict:
    policy = load_checkpoint(checkpoint)
    audit = audit_lifecycle_log(baseline, 1, policy)
    if (
        audit["lifecycle_anomalies"]
        or audit["pending_at_game_end"]
        or audit["terminal_coverage_fraction"] != 1.0
    ):
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

        candidate = next(
            (
                row for row in capture["candidates"]
                if row["action_identity"] == prediction["recommendation"]
            ),
            None,
        )
        if candidate is None:
            raise ValueError("learned recommendation is absent from Forge candidates")
        if candidate["replay_valid_count"] != capture["information_set_samples"]:
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
            "learned_action_targeted": _is_targeted(prediction["recommendation"]),
            "learned_target_public_semantics": candidate["target_public_semantics"],
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
            output,
            chosen["decision_index"],
            chosen["forge_action"],
            chosen["learned_action"],
        )

    report = {
        "schema_version": "stage8k-return-boundary-plan-v1",
        "mode": "single-learned-return-boundary-intervention",
        "model_id": policy.model_id,
        "actor_prefix": actor_prefix,
        "baseline_log": baseline.name,
        "intervention_planned": chosen is not None,
        "intervention": chosen,
        "request_file": output.name,
        "request_file_sha256": request_sha,
        "request_fields": [
            "decision_index",
            "expected_forge_complete_action_identity",
            "learned_complete_action_identity",
        ],
        "hidden_information_fields": 0,
        "early_search_substitution_allowed": False,
        "forge_phase_deferral_unchanged": True,
        "forge_referee": True,
        "promotion_allowed": False,
    }
    metadata.write_text(json.dumps(report, sort_keys=True, indent=2) + "\\n")
    return report


def _audit_controlled(path: Path, intervention: dict | None) -> dict:
    # Whole-log validator quarantines timeouts, exceptions and malformed Stage 7 data.
    label_log(path, expected_games=1)

    captures = {}
    arms = {}
    controls = {}
    returned = {}
    accepted = {}
    terminals = {}
    priority_events = []
    games = 0

    for raw in path.read_text().splitlines():
        line = raw.rstrip("\\n")
        if line.startswith(CAPTURE_PREFIX):
            event = _payload(line, CAPTURE_PREFIX)
            idx = event.get("decision_index")
            if type(idx) is not int or idx < 0 or idx in captures:
                raise ValueError("invalid or duplicate Stage 8K capture")
            captures[idx] = event
            continue

        if line.startswith(ARM_PREFIX):
            event = _payload(line, ARM_PREFIX)
            if event.get("schema_version") != "stage8k-return-boundary-armed-v1":
                raise ValueError("wrong Stage 8K armed schema")
            idx = event.get("decision_index")
            if type(idx) is not int or idx < 0 or idx in arms:
                raise ValueError("invalid or duplicate Stage 8K armed event")
            if event.get("early_substitution") is not False:
                raise ValueError("Stage 8K armed event changed Forge search early")
            arms[idx] = event
            continue

        if line.startswith(CONTROL_PREFIX):
            event = _payload(line, CONTROL_PREFIX)
            if event.get("schema_version") != "stage8k-return-boundary-selection-v1":
                raise ValueError("wrong Stage 8K control schema")
            idx = event.get("decision_index")
            if type(idx) is not int or idx < 0 or idx in controls:
                raise ValueError("invalid or duplicate Stage 8K control event")
            if (
                event.get("substitution_boundary") != "post-forge-plan-pre-return"
                or event.get("forge_search_unchanged") is not True
                or event.get("forge_phase_deferral_unchanged") is not True
                or event.get("forge_referee") is not True
                or event.get("promotion_allowed") is not False
            ):
                raise ValueError("Stage 8K control boundary metadata drift")
            controls[idx] = event
            continue

        if line.startswith(RETURN_PREFIX):
            event = _payload(line, RETURN_PREFIX)
            idx = event.get("capture_decision_index")
            if type(idx) is not int or idx < 0 or idx in returned:
                raise ValueError("invalid or duplicate Stage 8K returned action")
            if idx not in captures:
                raise ValueError("Stage 8K return references unknown capture")
            expected = captures[idx]["selected_action"]
            if intervention is not None and idx == intervention["decision_index"]:
                expected = intervention["learned_action"]
            if event.get("action_identity") != expected:
                raise ValueError("Stage 8K returned action identity drift")
            state = captures[idx]["public_state"]
            for key in ("turn", "phase", "acting_player_name"):
                if event.get(key) != state.get(key):
                    raise ValueError(f"Stage 8K return context drift: {key}")
            returned[idx] = event
            priority_events.append(event["priority_return_index"])
            continue

        if line.startswith(PASS_PREFIX):
            event = _payload(line, PASS_PREFIX)
            priority_events.append(event["priority_return_index"])
            continue

        if line.startswith(ACCEPT_PREFIX):
            event = _payload(line, ACCEPT_PREFIX)
            idx = event.get("capture_decision_index")
            if type(idx) is not int or idx < 0 or idx in accepted or idx not in returned:
                raise ValueError("invalid Stage 8K controller acceptance")
            ret = returned[idx]
            if (
                event.get("priority_return_index") != ret.get("priority_return_index")
                or event.get("action_identity") != ret.get("action_identity")
            ):
                raise ValueError("Stage 8K controller acceptance binding drift")
            for key in ("turn", "phase", "acting_player_name"):
                if event.get(key) != ret.get(key):
                    raise ValueError(f"Stage 8K acceptance context drift: {key}")
            if event.get("controller_return_value") is not True:
                raise ValueError("Stage 8K changed controller return semantics")
            if type(event.get("dispatch_success")) is not bool:
                raise ValueError("Stage 8K dispatch success must be boolean")
            accepted[idx] = event
            continue

        if line.startswith(TERMINAL_PREFIX):
            event = _payload(line, TERMINAL_PREFIX)
            idx = event.get("capture_decision_index")
            if type(idx) is not int or idx < 0 or idx in terminals or idx not in accepted:
                raise ValueError("invalid Stage 8K terminal event")
            acc = accepted[idx]
            if (
                event.get("priority_return_index") != acc.get("priority_return_index")
                or event.get("action_identity") != acc.get("action_identity")
                or event.get("acting_player_name") != acc.get("acting_player_name")
                or event.get("return_turn") != acc.get("turn")
                or event.get("return_phase") != acc.get("phase")
            ):
                raise ValueError("Stage 8K terminal binding drift")
            if event.get("outcome") not in ALLOWED_OUTCOMES:
                raise ValueError("unknown Stage 8K terminal outcome")
            terminals[idx] = event
            continue

        if line.startswith("Game Result: Game "):
            games += 1

    if games != 1:
        raise ValueError("Stage 8K controlled log does not contain exactly one game")
    if priority_events != list(range(len(priority_events))):
        raise ValueError("Stage 8K priority-return indices are not contiguous")
    if set(returned) != set(accepted):
        raise ValueError("Stage 8K returned/accepted action sets differ")
    if set(accepted) != set(terminals):
        raise ValueError("Stage 8K controller actions lack terminal coverage")

    anomaly_count = sum(
        event["outcome"] in ANOMALY_OUTCOMES for event in terminals.values()
    )
    if intervention is None:
        if arms or controls:
            raise ValueError("Stage 8K no-intervention game unexpectedly controlled play")
        applied = False
    else:
        idx = intervention["decision_index"]
        if set(arms) != {idx} or set(controls) != {idx}:
            raise ValueError("Stage 8K requires exactly one arm and one final substitution")
        arm = arms[idx]
        control = controls[idx]
        for event in (arm, control):
            if (
                event.get("acting_player_name") != intervention["actor"]
                or event.get("requested_action") != intervention["learned_action"]
                or event.get("forge_selected_action") != intervention["forge_action"]
            ):
                raise ValueError("Stage 8K control event differs from predeclared intervention")
        if captures[idx]["selected_action"] != intervention["forge_action"]:
            raise ValueError("Stage 8K modified Forge capture before return boundary")
        if idx not in returned or returned[idx]["action_identity"] != intervention["learned_action"]:
            raise ValueError("Stage 8K learned action did not cross return boundary")
        applied = True

    return {
        "captures": captures,
        "returned": returned,
        "accepted": accepted,
        "terminals": terminals,
        "intervention_applied": applied,
        "lifecycle_anomalies": anomaly_count,
        "pending_at_game_end": len(set(accepted) - set(terminals)),
        "terminal_coverage_fraction": len(terminals) / len(accepted) if accepted else None,
        "failed_dispatches": sum(not e["dispatch_success"] for e in accepted.values()),
    }


def compare_pair(
    checkpoint,
    baseline: Path,
    controlled: Path,
    plan_path: Path,
    request_path: Path,
    actor_prefix: str,
) -> dict:
    base_audit = audit_lifecycle_log(baseline, 1, checkpoint)
    if (
        base_audit["lifecycle_anomalies"] != 0
        or base_audit["pending_at_game_end"] != 0
        or base_audit["terminal_coverage_fraction"] != 1.0
        or base_audit["failed_dispatches"] != 0
    ):
        raise ValueError("Stage 8K baseline lifecycle is not clean")

    plan = json.loads(plan_path.read_text())
    if plan.get("schema_version") != "stage8k-return-boundary-plan-v1":
        raise ValueError("wrong Stage 8K plan schema")
    if plan.get("actor_prefix") != actor_prefix or plan.get("forge_referee") is not True:
        raise ValueError("Stage 8K plan actor/referee drift")
    if plan.get("promotion_allowed") is not False:
        raise ValueError("Stage 8K plan unexpectedly allows promotion")
    if plan.get("early_search_substitution_allowed") is not False:
        raise ValueError("Stage 8K plan permits early substitution")

    intervention = plan.get("intervention")
    ctrl = _audit_controlled(
        controlled, intervention if plan["intervention_planned"] else None
    )
    base_caps = _events(baseline, CAPTURE_PREFIX)
    base_by_idx = {row["decision_index"]: row for row in base_caps}
    ctrl_by_idx = ctrl["captures"]

    applied = False
    terminal_outcome = None
    if plan["intervention_planned"]:
        if not request_path.read_bytes():
            raise ValueError("planned Stage 8K intervention has empty request file")
        idx = intervention["decision_index"]
        if idx not in base_by_idx or idx not in ctrl_by_idx:
            raise ValueError("Stage 8K intervention capture is missing")
        for before_idx in range(idx):
            if base_by_idx.get(before_idx) != ctrl_by_idx.get(before_idx):
                raise ValueError("Stage 8K game drifted before the planned intervention")
        if _safe_signature(base_by_idx[idx]) != intervention["safe_capture_sha256"]:
            raise ValueError("Stage 8K baseline intervention state changed after planning")
        if base_by_idx[idx] != ctrl_by_idx[idx]:
            raise ValueError("Stage 8K changed live capture before the return boundary")
        if ctrl_by_idx[idx]["selected_action"] != intervention["forge_action"]:
            raise ValueError("Stage 8K did not preserve Forge's selected proposal")
        applied = ctrl["intervention_applied"]
        terminal_outcome = ctrl["terminals"][idx]["outcome"]
    else:
        if request_path.read_bytes():
            raise ValueError("Stage 8K no-intervention plan has a request file")

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
        "intervention_targeted": bool(
            intervention and intervention["learned_action_targeted"]
        ),
        "intervention_terminal_outcome": terminal_outcome,
        "baseline_winner": baseline_winner,
        "controlled_winner": controlled_winner,
        "controlled_side_baseline_score": baseline_score,
        "controlled_side_result_score": controlled_score,
        "controlled_side_score_delta": controlled_score - baseline_score,
        "pre_intervention_drift": 0,
        "invalid_requests_accepted": 0,
        "lifecycle_anomalies": ctrl["lifecycle_anomalies"],
        "forge_search_unchanged": True,
        "forge_phase_deferral_unchanged": True,
        "forge_referee": True,
        "promotion_allowed": False,
    }


def aggregate(
    pairs: list[dict],
    minimum_interventions: int,
    minimum_targeted_interventions: int,
) -> dict:
    planned = sum(p["intervention_planned"] for p in pairs)
    applied = sum(p["intervention_applied"] for p in pairs)
    targeted = sum(
        p["intervention_applied"] and p["intervention_targeted"] for p in pairs
    )
    anomalies = sum(p["lifecycle_anomalies"] for p in pairs)
    invalid = sum(p["invalid_requests_accepted"] for p in pairs)
    drift = sum(p["pre_intervention_drift"] for p in pairs)
    baseline_score = sum(p["controlled_side_baseline_score"] for p in pairs)
    controlled_score = sum(p["controlled_side_result_score"] for p in pairs)

    passed = (
        len(pairs) > 0
        and planned == applied
        and applied >= minimum_interventions
        and targeted >= minimum_targeted_interventions
        and anomalies == 0
        and invalid == 0
        and drift == 0
        and all(p["forge_search_unchanged"] for p in pairs)
        and all(p["forge_phase_deferral_unchanged"] for p in pairs)
    )
    return {
        "schema_version": "stage8k-return-boundary-result-v1",
        "mode": "single-learned-return-boundary-intervention",
        "promotion_allowed": False,
        "broader_learned_control_allowed": False,
        "forge_referee": True,
        "minimum_interventions": minimum_interventions,
        "minimum_targeted_interventions": minimum_targeted_interventions,
        "pairs": pairs,
        "totals": {
            "games": len(pairs),
            "interventions_planned": planned,
            "interventions_applied": applied,
            "targeted_interventions_applied": targeted,
            "lifecycle_anomalies": anomalies,
            "invalid_requests_accepted": invalid,
            "pre_intervention_drift": drift,
            "controlled_side_baseline_score": baseline_score,
            "controlled_side_result_score": controlled_score,
            "controlled_side_score_delta": controlled_score - baseline_score,
        },
        "safety_gate_passed": passed,
        "interpretation": (
            "This gate validates the learned-control execution boundary, including "
            "at least one targeted action, while Forge search, phase deferral, "
            "legality and rules execution remain authoritative. Outcome deltas are "
            "descriptive only and are not a strength-promotion criterion."
        ),
        "next_stage": (
            "If this gate passes, collect outcome-bearing public-only trajectories "
            "using the proven return-boundary controller, then train and evaluate "
            "an outcome-oriented policy/value signal before broader learned control."
        ),
    }


def main() -> None:
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
    compare.add_argument("--minimum-targeted-interventions", type=int, default=1)
    compare.add_argument("--output", required=True, type=Path)

    args = parser.parse_args()
    if args.command == "plan":
        result = plan_intervention(
            args.checkpoint,
            args.baseline,
            args.output,
            args.metadata,
            args.actor_prefix,
        )
        print(json.dumps({
            "intervention_planned": result["intervention_planned"],
            "intervention": result["intervention"],
            "model_id": result["model_id"],
        }, sort_keys=True))
        return

    lengths = {
        len(args.baseline),
        len(args.controlled),
        len(args.plans),
        len(args.requests),
    }
    if len(lengths) != 1:
        raise ValueError("Stage 8K pair input lists must have equal lengths")
    policy = load_checkpoint(args.checkpoint)
    pairs = [
        compare_pair(policy, b, c, p, r, args.actor_prefix)
        for b, c, p, r in zip(
            args.baseline, args.controlled, args.plans, args.requests
        )
    ]
    report = aggregate(
        pairs,
        args.minimum_interventions,
        args.minimum_targeted_interventions,
    )
    args.output.write_text(json.dumps(report, sort_keys=True, indent=2) + "\\n")
    print(json.dumps({
        "safety_gate_passed": report["safety_gate_passed"],
        **report["totals"],
        "promotion_allowed": report["promotion_allowed"],
    }, sort_keys=True))


if __name__ == "__main__":
    main()
