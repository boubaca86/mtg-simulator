"""Stage 8P: frozen Stage 8O model, public-only return-boundary planner."""
import hashlib
import json
import math
import re

from stage8d_shadow_policy import CAPTURE_PREFIX, load_checkpoint as load_validator, observe_capture, public_input
from stage8e_returned_action_audit import RETURN_PREFIX
from stage8h_lifecycle_audit import audit_log
from stage8k_return_boundary_intervention import _is_targeted, _request_bytes, _safe_signature, _winner
from stage8o_outcome_policy import features, load as load_outcome, predict_one

MODEL_ID = "90bbbd616e2509c3c85c318e5aded17cfe6e7592ad625bad2a1467136a596ba3"
VALIDATOR_ID = "6c4dd27f364aaa6c9e9100f88eb323eaa419dab0a49845ee0f748b9998746288"
SEEDS = (20261032, 20261033)
ORIENTATIONS = ("st-first", "red-first")
ACTOR = "Ai(1)-"
SCHEMA = "stage8p-reserved-evaluation-plan-v1"
MIN_MARGIN = 1e-6
TIE_TOLERANCE = 1e-12
NAME = re.compile(r"^stage8p-baseline-(st-first|red-first)-(\d{8})\.log$")


def load_models(outcome_path, validator_path):
    model = load_outcome(outcome_path)
    validator = load_validator(validator_path)
    if model["model_id"] != MODEL_ID or validator.model_id != VALIDATOR_ID:
        raise ValueError("Stage 8P frozen model/validator identity mismatch")
    return model, validator


def baseline_identity(path):
    match = NAME.fullmatch(path.name)
    if not match or int(match.group(2)) not in SEEDS:
        raise ValueError("Stage 8P requires an exact reserved seed and orientation")
    return match.group(1), int(match.group(2))


def _event(line, prefix):
    value = json.loads(line[len(prefix):])
    if not isinstance(value, dict):
        raise ValueError("Stage 8P event is not an object")
    return value


def _recommend(model, validator, capture):
    observe_capture(validator, capture)
    safe = public_input(capture["public_state"], capture["candidates"])
    scores = sorted((c["action_identity"], predict_one(model["weights"], features(safe, c)))
                    for c in safe["candidates"])
    if not scores or any(not math.isfinite(score) for _, score in scores):
        raise ValueError("Stage 8P invalid public prediction")
    maximum = max(score for _, score in scores)
    best = [action for action, score in scores
            if math.isclose(score, maximum, rel_tol=0.0, abs_tol=TIE_TOLERANCE)]
    if len(best) != 1:
        return None
    forge = capture["selected_action"]
    forge_score = next(score for action, score in scores if action == forge)
    if best[0] == forge or maximum <= forge_score + MIN_MARGIN:
        return None
    chosen = next(c for c in capture["candidates"] if c["action_identity"] == best[0])
    if chosen["replay_valid_count"] != capture["information_set_samples"]:
        raise ValueError("Stage 8P action not valid in all determinized worlds")
    return {
        "decision_index": capture["decision_index"],
        "actor": capture["public_state"]["acting_player_name"],
        "forge_action": forge,
        "learned_action": best[0],
        "learned_action_targeted": _is_targeted(best[0]),
        "predicted_margin": maximum - forge_score,
        "safe_capture_sha256": _safe_signature(capture),
    }


def choose(model, validator, baseline):
    baseline_identity(baseline)
    captures, returned = {}, set()
    # Chronological stream: no future game state or outcome used for selection.
    for line in baseline.read_text(encoding="utf-8", errors="replace").splitlines():
        if line.startswith(CAPTURE_PREFIX):
            capture = _event(line, CAPTURE_PREFIX)
            observe_capture(validator, capture)
            idx = capture["decision_index"]
            if idx in captures:
                raise ValueError("Stage 8P duplicate capture")
            captures[idx] = capture
        elif line.startswith(RETURN_PREFIX):
            event = _event(line, RETURN_PREFIX)
            idx = event.get("capture_decision_index")
            if idx not in captures or idx in returned:
                raise ValueError("Stage 8P missing/duplicate return")
            returned.add(idx)
            capture = captures[idx]
            if event.get("action_identity") != capture["selected_action"]:
                raise ValueError("Stage 8P baseline action differs from Forge")
            if not capture["public_state"]["acting_player_name"].startswith(ACTOR):
                continue
            chosen = _recommend(model, validator, capture)
            if chosen is not None:
                return chosen
    return None


def require_clean(baseline, validator):
    audit = audit_log(baseline, 1, validator)
    if (audit["lifecycle_anomalies"] or audit["pending_at_game_end"]
            or audit["failed_dispatches"] or audit["terminal_coverage_fraction"] != 1.0):
        raise ValueError("Stage 8P baseline Forge lifecycle is not clean")
    _winner(baseline)


def expected_plan(model, validator, baseline, request_path):
    orientation, seed = baseline_identity(baseline)
    chosen = choose(model, validator, baseline)
    raw = b"" if chosen is None else _request_bytes(
        chosen["decision_index"], chosen["forge_action"], chosen["learned_action"])
    return {
        "schema_version": SCHEMA,
        "model_id": model["model_id"],
        "validator_model_id": validator.model_id,
        "seed_family": seed,
        "orientation": orientation,
        "actor_prefix": ACTOR,
        "baseline_log": baseline.name,
        "intervention_planned": chosen is not None,
        "intervention": chosen,
        "request_file": request_path.name,
        "request_file_sha256": hashlib.sha256(raw).hexdigest(),
        "selection": "first-returned-strict-unique-positive-outcome-score-v1",
        "minimum_score_delta": MIN_MARGIN,
        "future_outcome_guided_selection": False,
        "forge_search_unchanged": True,
        "forge_phase_deferral_unchanged": True,
        "forge_referee": True,
        "promotion_allowed": False,
        "broader_learned_control_allowed": False,
    }, raw


def plan(model, validator, baseline, request_path, metadata_path):
    require_clean(baseline, validator)
    payload, raw = expected_plan(model, validator, baseline, request_path)
    request_path.write_bytes(raw)
    metadata_path.write_text(json.dumps(payload, sort_keys=True, indent=2) + "\n")
    return payload
