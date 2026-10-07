"""Stage 8P precommitted reserved outcome decision gate."""
from stage8p_reserved_planner import MODEL_ID, ORIENTATIONS, SEEDS

REPORT_SCHEMA = "stage8p-reserved-evaluation-report-v1"
MIN_APPLIED = 2
MIN_PAIRED_SCORE_GAIN = 1.0


def aggregate(pairs):
    expected = {(seed, orientation) for seed in SEEDS for orientation in ORIENTATIONS}
    actual = [(p["seed_family"], p["orientation"]) for p in pairs]
    if len(pairs) != 4 or len(set(actual)) != 4 or set(actual) != expected:
        raise ValueError("Stage 8P requires four distinct reserved game pairs")
    planned = sum(bool(p["intervention_planned"]) for p in pairs)
    applied = sum(bool(p["intervention_applied"]) for p in pairs)
    delta = sum(p["paired_score_delta"] for p in pairs)
    safety = (
        planned == applied and applied >= MIN_APPLIED
        and all(p["failed_dispatches"] == 0
                and p["lifecycle_anomalies"] == 0
                and p["pre_intervention_drift"] == 0
                and p["forge_referee"] is True
                and p["promotion_allowed"] is False for p in pairs)
    )
    primary = delta >= MIN_PAIRED_SCORE_GAIN
    return {
        "schema_version": REPORT_SCHEMA,
        "model_id": MODEL_ID,
        "reserved_seed_families": list(SEEDS),
        "pairs": sorted(pairs, key=lambda p: (p["seed_family"], p["orientation"])),
        "totals": {
            "games": 4,
            "interventions_planned": planned,
            "interventions_applied": applied,
            "targeted_interventions_applied": sum(
                bool(p["intervention_applied"] and p["intervention_targeted"])
                for p in pairs),
            "baseline_side_score": sum(p["baseline_side_score"] for p in pairs),
            "controlled_side_score": sum(p["controlled_side_score"] for p in pairs),
            "paired_score_delta": delta,
        },
        "safety_gate_passed": safety,
        "primary_game_result_gate_passed": primary,
        "evaluation_gate_passed": safety and primary,
        "promotion_allowed": False,
        "broader_learned_control_allowed": False,
        "strength_claim_allowed": False,
        "forge_referee": True,
        "interpretation": (
            "One-shot four-pair reserved pilot. A positive result is not "
            "expert-human-level evidence or authority for broader control."
        ),
    }
