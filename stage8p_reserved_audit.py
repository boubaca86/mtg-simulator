"""Stage 8P exact Forge boundary and lifecycle audit."""
import json
from stage8k_return_boundary_intervention import (
    _audit_controlled, _safe_signature, _side_score, _verify_unchanged_prefix, _winner,
)
from stage8d_shadow_policy import CAPTURE_PREFIX
from stage8p_reserved_planner import ACTOR, _event, expected_plan, require_clean


def compare_pair(model, validator, baseline, controlled, plan_path, request_path):
    require_clean(baseline, validator)
    expected, raw = expected_plan(model, validator, baseline, request_path)
    if json.loads(plan_path.read_text()) != expected or request_path.read_bytes() != raw:
        raise ValueError("Stage 8P plan or request differs from frozen public rule")
    chosen = expected["intervention"]
    # The baseline audit requires every return to equal Forge's proposal.
    # Controlled play intentionally differs at exactly the frozen intervention.
    # Stage 8K validates that exception plus the complete Stage 7/F/H lifecycle.
    ctrl = _audit_controlled(controlled, chosen, validator)
    _verify_unchanged_prefix(baseline, controlled, chosen)
    if (ctrl["failed_dispatches"] or ctrl["lifecycle_anomalies"]
            or ctrl["pending_at_game_end"] or ctrl["terminal_coverage_fraction"] != 1.0):
        raise ValueError("Stage 8P failed Forge dispatch or terminal lifecycle")
    if bool(ctrl["intervention_applied"]) != bool(chosen):
        raise ValueError("Stage 8P intervention not applied as planned")
    if chosen:
        idx = chosen["decision_index"]
        original = {
            e["decision_index"]: e for e in (
                _event(line, CAPTURE_PREFIX)
                for line in baseline.read_text().splitlines()
                if line.startswith(CAPTURE_PREFIX))
        }
        if idx not in original or idx not in ctrl["captures"]:
            raise ValueError("Stage 8P intervention capture missing")
        if (_safe_signature(original[idx]) != chosen["safe_capture_sha256"]
                or original[idx] != ctrl["captures"][idx]):
            raise ValueError("Stage 8P pre-return Forge capture changed")
        if any(ctrl[phase][idx]["action_identity"] != chosen["learned_action"]
               for phase in ("returned", "accepted", "terminals")):
            raise ValueError("Stage 8P action identity lost during Forge lifecycle")
    base = _side_score(_winner(baseline), ACTOR)
    test = _side_score(_winner(controlled), ACTOR)
    return {
        "seed_family": expected["seed_family"],
        "orientation": expected["orientation"],
        "baseline_log": baseline.name,
        "controlled_log": controlled.name,
        "intervention_planned": bool(chosen),
        "intervention_applied": ctrl["intervention_applied"],
        "intervention_targeted": bool(chosen and chosen["learned_action_targeted"]),
        "baseline_side_score": base,
        "controlled_side_score": test,
        "paired_score_delta": test-base,
        "failed_dispatches": ctrl["failed_dispatches"],
        "lifecycle_anomalies": ctrl["lifecycle_anomalies"],
        "pre_intervention_drift": 0,
        "forge_referee": True,
        "promotion_allowed": False,
    }
