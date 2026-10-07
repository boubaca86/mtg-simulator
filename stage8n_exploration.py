#!/usr/bin/env python3
"""Stage 8N: deterministic public-only single-action exploration.

Purpose: expand outcome-bearing development data after Stage 8M's internal
outcome-prediction diagnostic underperformed a constant baseline.

The exploration choice is deliberately model-independent. It selects at most one
already-captured, replay-valid legal alternative action at the proven Stage 8K
post-Forge-plan/pre-return boundary. Forge remains the sole rules referee and
executor. Reserved evaluation seeds are never used by this module.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

from stage8d_shadow_policy import CAPTURE_PREFIX, load_checkpoint, observe_capture
from stage8e_returned_action_audit import RETURN_PREFIX
from stage8h_lifecycle_audit import audit_log as audit_lifecycle_log
from stage8k_return_boundary_intervention import (
    _audit_controlled,
    _is_targeted,
    _request_bytes,
    _safe_signature,
    _side_score,
    _verify_unchanged_prefix,
    _winner,
)

PLAN_SCHEMA = "stage8n-public-exploration-plan-v1"
RESULT_SCHEMA = "stage8n-public-exploration-result-v1"
EXPLORATION_RULE = "first-returned-public-alternative-hash-v1"
DEVELOPMENT_SEEDS = tuple(range(20261034, 20261042))
RESERVED_EVALUATION_SEEDS = (20261032, 20261033)


def _payload(line: str, prefix: str) -> dict:
    value = json.loads(line[len(prefix):])
    if not isinstance(value, dict):
        raise ValueError("Stage 8N event must be a JSON object")
    return value


def _seed_from_path(path: Path) -> int:
    match = re.search(r"(\d{8})(?=\.log$)", path.name)
    if not match:
        raise ValueError("Stage 8N source log filename lacks development seed")
    return int(match.group(1))


def _selection_hash(seed: int, decision_index: int, action_identity: str) -> str:
    raw = (
        f"{EXPLORATION_RULE}\0{seed}\0{decision_index}\0{action_identity}"
    ).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()


def _choose_from_capture(capture: dict, corpus_seed: int) -> dict | None:
    selected = capture.get("selected_action")
    samples = capture.get("information_set_samples")
    if type(samples) is not int or samples <= 0:
        raise ValueError("Stage 8N capture lacks information-set sample count")

    alternatives = []
    for candidate in capture.get("candidates", []):
        if not isinstance(candidate, dict):
            raise ValueError("Stage 8N candidate must be an object")
        action = candidate.get("action_identity")
        if action == selected:
            continue
        if candidate.get("replay_valid_count") != samples:
            continue
        if not isinstance(action, str) or not action:
            raise ValueError("Stage 8N candidate lacks complete action identity")
        alternatives.append(candidate)

    if not alternatives:
        return None

    targeted = [candidate for candidate in alternatives if _is_targeted(candidate["action_identity"])]
    untargeted = [candidate for candidate in alternatives if not _is_targeted(candidate["action_identity"])]

    prefer_targeted = corpus_seed % 2 == 0
    if prefer_targeted and targeted:
        pool = targeted
        pool_kind = "targeted-preferred"
    elif not prefer_targeted and untargeted:
        pool = untargeted
        pool_kind = "untargeted-preferred"
    else:
        pool = alternatives
        pool_kind = "fallback-any-public-alternative"

    decision_index = capture["decision_index"]
    chosen = min(
        pool,
        key=lambda candidate: (
            _selection_hash(corpus_seed, decision_index, candidate["action_identity"]),
            candidate["action_identity"],
        ),
    )
    action = chosen["action_identity"]
    return {
        "decision_index": decision_index,
        "actor": capture["public_state"]["acting_player_name"],
        "turn": capture["public_state"]["turn"],
        "phase": capture["public_state"]["phase"],
        "forge_action": selected,
        "exploration_action": action,
        "exploration_action_targeted": _is_targeted(action),
        "exploration_target_public_semantics": chosen["target_public_semantics"],
        "candidate_count": len(capture["candidates"]),
        "alternative_count": len(alternatives),
        "selection_pool": pool_kind,
        "selection_digest": _selection_hash(corpus_seed, decision_index, action),
        "safe_capture_sha256": _safe_signature(capture),
    }


def choose_exploration(policy, baseline: Path, actor_prefix: str, corpus_seed: int) -> dict | None:
    if corpus_seed not in DEVELOPMENT_SEEDS:
        raise ValueError("Stage 8N corpus seed is not a predeclared development family")
    if corpus_seed in RESERVED_EVALUATION_SEEDS:
        raise ValueError("Stage 8N must not use reserved evaluation seeds")
    if _seed_from_path(baseline) != corpus_seed:
        raise ValueError("Stage 8N baseline filename/seed mismatch")

    captures: dict[int, dict] = {}
    for raw in baseline.read_text(encoding="utf-8", errors="replace").splitlines():
        if raw.startswith(CAPTURE_PREFIX):
            capture = _payload(raw, CAPTURE_PREFIX)
            observe_capture(policy, capture)
            idx = capture.get("decision_index")
            if type(idx) is not int or idx < 0 or idx in captures:
                raise ValueError("invalid or duplicate Stage 8N capture")
            captures[idx] = capture
            continue

        if raw.startswith(RETURN_PREFIX):
            returned = _payload(raw, RETURN_PREFIX)
            idx = returned.get("capture_decision_index")
            if idx not in captures:
                raise ValueError("Stage 8N return references unknown capture")
            capture = captures[idx]
            actor = str(capture.get("public_state", {}).get("acting_player_name", ""))
            if not actor.startswith(actor_prefix):
                continue
            chosen = _choose_from_capture(capture, corpus_seed)
            if chosen is not None:
                return chosen

    return None


def _request_digest_bytes(chosen: dict | None) -> tuple[bytes, str]:
    if chosen is None:
        raw = b""
    else:
        raw = _request_bytes(
            chosen["decision_index"],
            chosen["forge_action"],
            chosen["exploration_action"],
        )
    return raw, hashlib.sha256(raw).hexdigest()


def plan_exploration(
    checkpoint: Path,
    baseline: Path,
    output: Path,
    metadata: Path,
    actor_prefix: str,
    corpus_seed: int,
) -> dict:
    policy = load_checkpoint(checkpoint)
    audit = audit_lifecycle_log(baseline, 1, policy)
    if (
        audit["lifecycle_anomalies"]
        or audit["pending_at_game_end"]
        or audit["failed_dispatches"]
        or audit["terminal_coverage_fraction"] != 1.0
    ):
        raise ValueError("Stage 8N baseline lifecycle is not clean")

    chosen = choose_exploration(policy, baseline, actor_prefix, corpus_seed)
    raw, digest = _request_digest_bytes(chosen)
    output.write_bytes(raw)

    report = {
        "schema_version": PLAN_SCHEMA,
        "mode": "single-deterministic-public-exploration-intervention",
        "exploration_rule": EXPLORATION_RULE,
        "validation_model_id": policy.model_id,
        "corpus_seed": corpus_seed,
        "actor_prefix": actor_prefix,
        "baseline_log": baseline.name,
        "intervention_planned": chosen is not None,
        "intervention": chosen,
        "request_file": output.name,
        "request_file_sha256": digest,
        "model_guided_selection": False,
        "outcome_guided_selection": False,
        "future_state_guided_selection": False,
        "hidden_information_fields": 0,
        "early_search_substitution_allowed": False,
        "forge_phase_deferral_unchanged": True,
        "forge_referee": True,
        "promotion_allowed": False,
        "broader_learned_control_allowed": False,
    }
    metadata.write_text(json.dumps(report, sort_keys=True, indent=2) + "\n")
    return report


def _as_stage8k_intervention(chosen: dict | None) -> dict | None:
    if chosen is None:
        return None
    return {
        "decision_index": chosen["decision_index"],
        "actor": chosen["actor"],
        "forge_action": chosen["forge_action"],
        "learned_action": chosen["exploration_action"],
    }


def compare_pair(
    checkpoint,
    baseline: Path,
    controlled: Path,
    plan_path: Path,
    request_path: Path,
    actor_prefix: str,
    corpus_seed: int,
) -> dict:
    policy = checkpoint if hasattr(checkpoint, "predict") else load_checkpoint(checkpoint)

    base_audit = audit_lifecycle_log(baseline, 1, policy)
    if (
        base_audit["lifecycle_anomalies"]
        or base_audit["pending_at_game_end"]
        or base_audit["failed_dispatches"]
        or base_audit["terminal_coverage_fraction"] != 1.0
    ):
        raise ValueError("Stage 8N baseline lifecycle is not clean")

    plan = json.loads(plan_path.read_text())
    if plan.get("schema_version") != PLAN_SCHEMA:
        raise ValueError("wrong Stage 8N plan schema")
    if plan.get("exploration_rule") != EXPLORATION_RULE:
        raise ValueError("wrong Stage 8N exploration rule")
    if plan.get("validation_model_id") != policy.model_id:
        raise ValueError("Stage 8N plan validation model drift")
    if plan.get("corpus_seed") != corpus_seed:
        raise ValueError("Stage 8N plan corpus seed drift")
    if plan.get("actor_prefix") != actor_prefix:
        raise ValueError("Stage 8N plan actor drift")
    if plan.get("forge_referee") is not True or plan.get("promotion_allowed") is not False:
        raise ValueError("Stage 8N plan referee/promotion drift")
    if plan.get("broader_learned_control_allowed") is not False:
        raise ValueError("Stage 8N plan unexpectedly expands learned control")
    if any(plan.get(key) is not False for key in (
        "model_guided_selection",
        "outcome_guided_selection",
        "future_state_guided_selection",
        "early_search_substitution_allowed",
    )):
        raise ValueError("Stage 8N exploration selection is not independent/public-only")

    chosen = choose_exploration(policy, baseline, actor_prefix, corpus_seed)
    if plan.get("intervention_planned") != (chosen is not None):
        raise ValueError("Stage 8N plan intervention flag drift")
    if plan.get("intervention") != chosen:
        raise ValueError("Stage 8N plan differs from deterministic public exploration")

    expected_raw, expected_digest = _request_digest_bytes(chosen)
    raw = request_path.read_bytes()
    if raw != expected_raw or hashlib.sha256(raw).hexdigest() != expected_digest:
        raise ValueError("Stage 8N request bytes differ from deterministic plan")
    if plan.get("request_file_sha256") != expected_digest:
        raise ValueError("Stage 8N request digest drift")

    shim = _as_stage8k_intervention(chosen)
    ctrl = _audit_controlled(controlled, shim, policy)
    _verify_unchanged_prefix(baseline, controlled, shim)

    applied = False
    terminal_outcome = None
    if chosen is not None:
        idx = chosen["decision_index"]
        baseline_captures = {
            event["decision_index"]: event
            for event in (
                _payload(line, CAPTURE_PREFIX)
                for line in baseline.read_text().splitlines()
                if line.startswith(CAPTURE_PREFIX)
            )
        }
        if idx not in baseline_captures or idx not in ctrl["captures"]:
            raise ValueError("Stage 8N intervention capture missing")
        if _safe_signature(baseline_captures[idx]) != chosen["safe_capture_sha256"]:
            raise ValueError("Stage 8N baseline capture changed after planning")
        if baseline_captures[idx] != ctrl["captures"][idx]:
            raise ValueError("Stage 8N changed Forge capture before return boundary")
        if ctrl["captures"][idx]["selected_action"] != chosen["forge_action"]:
            raise ValueError("Stage 8N changed Forge-selected proposal")
        if not ctrl["intervention_applied"]:
            raise ValueError("Stage 8N planned exploration did not cross return boundary")
        returned = ctrl["returned"].get(idx)
        if returned is None or returned.get("action_identity") != chosen["exploration_action"]:
            raise ValueError("Stage 8N exploration action was not actually returned")
        terminal_outcome = ctrl["terminals"][idx]["outcome"]
        applied = True
    else:
        if ctrl["intervention_applied"]:
            raise ValueError("Stage 8N no-plan game unexpectedly applied control")

    baseline_winner = _winner(baseline)
    controlled_winner = _winner(controlled)
    baseline_score = _side_score(baseline_winner, actor_prefix)
    controlled_score = _side_score(controlled_winner, actor_prefix)

    return {
        "baseline_log": baseline.name,
        "controlled_log": controlled.name,
        "plan_file": plan_path.name,
        "request_file": request_path.name,
        "corpus_seed": corpus_seed,
        "intervention_planned": chosen is not None,
        "intervention_applied": applied,
        "intervention_targeted": bool(chosen and chosen["exploration_action_targeted"]),
        "intervention_terminal_outcome": terminal_outcome,
        "baseline_winner": baseline_winner,
        "controlled_winner": controlled_winner,
        "controlled_side_baseline_score": baseline_score,
        "controlled_side_result_score": controlled_score,
        "controlled_side_score_delta": controlled_score - baseline_score,
        "pre_intervention_drift": 0,
        "invalid_requests_accepted": 0,
        "lifecycle_anomalies": ctrl["lifecycle_anomalies"],
        "failed_dispatches": ctrl["failed_dispatches"],
        "model_guided_selection": False,
        "outcome_guided_selection": False,
        "forge_search_unchanged": True,
        "forge_phase_deferral_unchanged": True,
        "forge_referee": True,
        "promotion_allowed": False,
    }


def aggregate(
    pairs: list[dict],
    minimum_interventions: int = 12,
    minimum_targeted_interventions: int = 2,
    minimum_untargeted_interventions: int = 6,
) -> dict:
    if not pairs:
        raise ValueError("Stage 8N aggregate requires game pairs")
    planned = sum(bool(pair["intervention_planned"]) for pair in pairs)
    applied = sum(bool(pair["intervention_applied"]) for pair in pairs)
    targeted = sum(
        bool(pair["intervention_applied"] and pair["intervention_targeted"])
        for pair in pairs
    )
    untargeted = sum(
        bool(pair["intervention_applied"] and not pair["intervention_targeted"])
        for pair in pairs
    )
    anomalies = sum(pair["lifecycle_anomalies"] for pair in pairs)
    failed = sum(pair["failed_dispatches"] for pair in pairs)
    invalid = sum(pair["invalid_requests_accepted"] for pair in pairs)
    drift = sum(pair["pre_intervention_drift"] for pair in pairs)
    baseline_score = sum(pair["controlled_side_baseline_score"] for pair in pairs)
    controlled_score = sum(pair["controlled_side_result_score"] for pair in pairs)

    seeds = sorted({pair["corpus_seed"] for pair in pairs})
    passed = (
        seeds == list(DEVELOPMENT_SEEDS)
        and planned == applied
        and applied >= minimum_interventions
        and targeted >= minimum_targeted_interventions
        and untargeted >= minimum_untargeted_interventions
        and anomalies == 0
        and failed == 0
        and invalid == 0
        and drift == 0
        and all(pair["forge_search_unchanged"] for pair in pairs)
        and all(pair["forge_phase_deferral_unchanged"] for pair in pairs)
        and all(pair["forge_referee"] for pair in pairs)
        and all(pair["model_guided_selection"] is False for pair in pairs)
        and all(pair["outcome_guided_selection"] is False for pair in pairs)
    )
    return {
        "schema_version": RESULT_SCHEMA,
        "mode": "development-only-public-action-exploration",
        "development_seeds": list(DEVELOPMENT_SEEDS),
        "reserved_untouched_evaluation_seeds": list(RESERVED_EVALUATION_SEEDS),
        "minimum_interventions": minimum_interventions,
        "minimum_targeted_interventions": minimum_targeted_interventions,
        "minimum_untargeted_interventions": minimum_untargeted_interventions,
        "pairs": pairs,
        "totals": {
            "games": len(pairs),
            "interventions_planned": planned,
            "interventions_applied": applied,
            "targeted_interventions_applied": targeted,
            "untargeted_interventions_applied": untargeted,
            "lifecycle_anomalies": anomalies,
            "failed_dispatches": failed,
            "invalid_requests_accepted": invalid,
            "pre_intervention_drift": drift,
            "controlled_side_baseline_score": baseline_score,
            "controlled_side_result_score": controlled_score,
            "controlled_side_score_delta": controlled_score - baseline_score,
        },
        "safety_gate_passed": passed,
        "training_performed": False,
        "promotion_allowed": False,
        "broader_learned_control_allowed": False,
        "forge_referee": True,
        "strength_claim_allowed": False,
        "interpretation": (
            "This gate certifies development-data exploration integrity only. "
            "Outcome deltas are descriptive; the exploration action was chosen "
            "without a learned model, without outcome information, and only from "
            "Forge's captured replay-valid legal candidate set."
        ),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)

    plan = sub.add_parser("plan")
    plan.add_argument("checkpoint", type=Path)
    plan.add_argument("baseline", type=Path)
    plan.add_argument("--corpus-seed", required=True, type=int)
    plan.add_argument("--actor-prefix", default="Ai(1)-")
    plan.add_argument("--output", required=True, type=Path)
    plan.add_argument("--metadata", required=True, type=Path)

    compare = sub.add_parser("compare")
    compare.add_argument("checkpoint", type=Path)
    compare.add_argument("--baseline", nargs="+", required=True, type=Path)
    compare.add_argument("--controlled", nargs="+", required=True, type=Path)
    compare.add_argument("--plans", nargs="+", required=True, type=Path)
    compare.add_argument("--requests", nargs="+", required=True, type=Path)
    compare.add_argument("--corpus-seeds", nargs="+", required=True, type=int)
    compare.add_argument("--actor-prefix", default="Ai(1)-")
    compare.add_argument("--minimum-interventions", type=int, default=12)
    compare.add_argument("--minimum-targeted-interventions", type=int, default=2)
    compare.add_argument("--minimum-untargeted-interventions", type=int, default=6)
    compare.add_argument("--output", required=True, type=Path)

    args = parser.parse_args()
    if args.command == "plan":
        result = plan_exploration(
            args.checkpoint,
            args.baseline,
            args.output,
            args.metadata,
            args.actor_prefix,
            args.corpus_seed,
        )
        print(json.dumps({
            "intervention_planned": result["intervention_planned"],
            "intervention": result["intervention"],
            "corpus_seed": result["corpus_seed"],
            "exploration_rule": result["exploration_rule"],
        }, sort_keys=True))
        return

    lengths = {
        len(args.baseline),
        len(args.controlled),
        len(args.plans),
        len(args.requests),
        len(args.corpus_seeds),
    }
    if len(lengths) != 1:
        raise ValueError("Stage 8N compare input lists must have equal lengths")

    policy = load_checkpoint(args.checkpoint)
    pairs = [
        compare_pair(policy, baseline, controlled, plan_path, request, args.actor_prefix, seed)
        for baseline, controlled, plan_path, request, seed in zip(
            args.baseline,
            args.controlled,
            args.plans,
            args.requests,
            args.corpus_seeds,
        )
    ]
    report = aggregate(
        pairs,
        args.minimum_interventions,
        args.minimum_targeted_interventions,
        args.minimum_untargeted_interventions,
    )
    args.output.write_text(json.dumps(report, sort_keys=True, indent=2) + "\n")
    print(json.dumps({
        "safety_gate_passed": report["safety_gate_passed"],
        **report["totals"],
        "promotion_allowed": report["promotion_allowed"],
    }, sort_keys=True))


if __name__ == "__main__":
    main()
