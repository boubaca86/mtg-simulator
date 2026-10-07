#!/usr/bin/env python3
"""Stage 8L: collect public-only executed-action trajectories with game outcomes.

The learner-facing input is produced through Stage 8D's explicit public allowlist.
Search scores, Forge choices, terminal outcomes, seeds and hidden-zone identities are
not learner inputs. Outcome and execution data are labels/provenance only.

Forge remains the sole rules referee and action executor.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path

from stage7_dataset_validation import reject_hidden
from stage7_label_outcomes import label_log
from stage8d_shadow_policy import CAPTURE_PREFIX, public_input
from stage8e_returned_action_audit import RETURN_PREFIX
from stage8f_acceptance_audit import ACCEPT_PREFIX
from stage8h_lifecycle_audit import (
    ALLOWED_OUTCOMES,
    ANOMALY_OUTCOMES,
    TERMINAL_PREFIX,
)
from stage8k_return_boundary_intervention import ARM_PREFIX, CONTROL_PREFIX, _is_targeted

SCHEMA_VERSION = "stage8l-outcome-trajectory-v1"
MANIFEST_VERSION = "stage8l-outcome-trajectory-manifest-v1"
EXPLORATION_SEEDS = (20261028, 20261029, 20261030, 20261031)
RESERVED_EVALUATION_SEEDS = (20261032, 20261033)
FORBIDDEN_MODEL_KEYS = {
    "aggregate_score",
    "model_score",
    "selected_action",
    "forge_proposed_action",
    "behavior_action",
    "game_result",
    "terminal_action_outcome",
    "terminal_winner_player",
    "run_seed",
    "decision_index",
    "priority_return_index",
    "corpus_seed",
}


def canonical_json(value) -> str:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), allow_nan=False)


def _payload(line: str, prefix: str) -> dict:
    value = json.loads(line[len(prefix):])
    if not isinstance(value, dict):
        raise ValueError("Stage 8L audit event must be a JSON object")
    return value


def _walk_keys(value):
    if isinstance(value, dict):
        for key, child in value.items():
            yield key
            yield from _walk_keys(child)
    elif isinstance(value, list):
        for child in value:
            yield from _walk_keys(child)


def validate_model_input(value: dict) -> dict:
    if set(value) != {"public_state", "candidates"}:
        raise ValueError("Stage 8L model input must contain only public_state/candidates")
    reject_hidden(value)
    bad = FORBIDDEN_MODEL_KEYS & set(_walk_keys(value))
    if bad:
        raise ValueError(f"Stage 8L label/audit leakage into model input: {sorted(bad)}")
    candidates = value.get("candidates")
    if not isinstance(candidates, list) or not candidates:
        raise ValueError("Stage 8L model input requires legal candidates")
    return value


def validate_trajectory_row(row: dict) -> dict:
    if row.get("schema_version") != SCHEMA_VERSION:
        raise ValueError("wrong Stage 8L row schema")
    model_input = validate_model_input(row.get("model_input", {}))
    candidate_ids = {c.get("action_identity") for c in model_input["candidates"]}
    behavior = row.get("behavior_action")
    if behavior not in candidate_ids:
        raise ValueError("Stage 8L behavior action is absent from public candidate set")
    if row.get("behavior_source") not in {"forge", "learned-return-boundary"}:
        raise ValueError("unknown Stage 8L behavior source")
    labels = row.get("labels")
    if not isinstance(labels, dict) or set(labels) != {
        "game_result", "terminal_action_outcome"
    }:
        raise ValueError("Stage 8L labels must be separate from model input")
    if labels["game_result"] not in (0.0, 0.5, 1.0):
        raise ValueError("invalid Stage 8L game outcome")
    if labels["terminal_action_outcome"] not in ALLOWED_OUTCOMES:
        raise ValueError("invalid Stage 8L terminal outcome")
    if labels["terminal_action_outcome"] in ANOMALY_OUTCOMES:
        raise ValueError("Stage 8L refuses anomalous action lifecycles")
    audit = row.get("audit")
    if not isinstance(audit, dict):
        raise ValueError("Stage 8L audit metadata missing")
    if (
        audit.get("forge_referee") is not True
        or audit.get("return_boundary_verified") is not True
        or audit.get("controller_acceptance_verified") is not True
        or audit.get("terminal_binding_verified") is not True
        or audit.get("hidden_information_fields") != 0
        or audit.get("promotion_allowed") is not False
    ):
        raise ValueError("Stage 8L safety provenance drift")
    forge_action = audit.get("forge_proposed_action")
    if row["behavior_source"] == "forge" and behavior != forge_action:
        raise ValueError("Forge behavior row disagrees with Forge proposal")
    if row["behavior_source"] == "learned-return-boundary" and behavior == forge_action:
        raise ValueError("learned behavior row did not actually differ from Forge")
    return row


def _single_winner(path: Path) -> int | None:
    labeled = label_log(path, expected_games=1, samples=3)
    winners = {row["terminal_winner_player"] for row in labeled}
    if len(winners) != 1:
        raise ValueError("Stage 8L game has inconsistent terminal winner provenance")
    return next(iter(winners))


def collect_log(path: Path) -> list[dict]:
    winner = _single_winner(path)
    captures: dict[int, dict] = {}
    returned: dict[int, dict] = {}
    accepted: dict[int, dict] = {}
    terminals: dict[int, dict] = {}
    armed: dict[int, dict] = {}
    controls: dict[int, dict] = {}

    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if raw.startswith(CAPTURE_PREFIX):
            event = _payload(raw, CAPTURE_PREFIX)
            idx = event.get("decision_index")
            if type(idx) is not int or idx < 0 or idx in captures:
                raise ValueError("invalid or duplicate Stage 8L capture")
            captures[idx] = event
        elif raw.startswith(RETURN_PREFIX):
            event = _payload(raw, RETURN_PREFIX)
            idx = event.get("capture_decision_index")
            if type(idx) is not int or idx < 0 or idx in returned:
                raise ValueError("invalid or duplicate Stage 8L returned action")
            returned[idx] = event
        elif raw.startswith(ACCEPT_PREFIX):
            event = _payload(raw, ACCEPT_PREFIX)
            idx = event.get("capture_decision_index")
            if type(idx) is not int or idx < 0 or idx in accepted:
                raise ValueError("invalid or duplicate Stage 8L acceptance")
            accepted[idx] = event
        elif raw.startswith(TERMINAL_PREFIX):
            event = _payload(raw, TERMINAL_PREFIX)
            idx = event.get("capture_decision_index")
            if type(idx) is not int or idx < 0 or idx in terminals:
                raise ValueError("invalid or duplicate Stage 8L terminal")
            terminals[idx] = event
        elif raw.startswith(ARM_PREFIX):
            event = _payload(raw, ARM_PREFIX)
            idx = event.get("decision_index")
            if type(idx) is not int or idx < 0 or idx in armed:
                raise ValueError("invalid or duplicate Stage 8L arm event")
            armed[idx] = event
        elif raw.startswith(CONTROL_PREFIX):
            event = _payload(raw, CONTROL_PREFIX)
            idx = event.get("decision_index")
            if type(idx) is not int or idx < 0 or idx in controls:
                raise ValueError("invalid or duplicate Stage 8L control event")
            controls[idx] = event

    if not returned:
        raise ValueError("Stage 8L source contains no returned Forge actions")
    if set(returned) != set(accepted) or set(accepted) != set(terminals):
        raise ValueError("Stage 8L requires complete return/accept/terminal coverage")

    rows = []
    source_sha256 = hashlib.sha256(path.read_bytes()).hexdigest()
    for idx, ret in sorted(
        returned.items(), key=lambda pair: pair[1].get("priority_return_index", -1)
    ):
        if idx not in captures:
            raise ValueError("Stage 8L returned action references unknown capture")
        capture = captures[idx]
        acceptance = accepted[idx]
        terminal = terminals[idx]
        if capture.get("schema_version") != "stage8-capture-v1":
            raise ValueError("wrong Stage 8 capture schema")
        if capture.get("information_set_samples") != 3:
            raise ValueError("Stage 8L requires three-world candidate replay")
        state = capture.get("public_state", {})
        if state.get("schema_version") != "stage7d-v1" or state.get("search_policy") != "fixed-root-v1":
            raise ValueError("Stage 8L requires current fixed-root public capture")

        executed = ret.get("action_identity")
        if executed not in {c.get("action_identity") for c in capture.get("candidates", [])}:
            raise ValueError("Stage 8L executed action was not in Forge's captured legal set")
        if (
            acceptance.get("action_identity") != executed
            or terminal.get("action_identity") != executed
            or acceptance.get("priority_return_index") != ret.get("priority_return_index")
            or terminal.get("priority_return_index") != ret.get("priority_return_index")
        ):
            raise ValueError("Stage 8L action identity/lifecycle binding drift")
        if acceptance.get("dispatch_success") is not True:
            raise ValueError("Stage 8L refuses failed controller dispatch")
        if terminal.get("outcome") in ANOMALY_OUTCOMES:
            raise ValueError("Stage 8L refuses lifecycle anomalies")

        forge_action = capture.get("selected_action")
        behavior_source = "forge" if executed == forge_action else "learned-return-boundary"
        if behavior_source == "learned-return-boundary":
            if set(controls) != {idx} or set(armed) != {idx}:
                raise ValueError("Stage 8L learned action lacks unique Stage 8K control provenance")
            control = controls[idx]
            arm = armed[idx]
            for event in (control, arm):
                if (
                    event.get("requested_action") != executed
                    or event.get("forge_selected_action") != forge_action
                ):
                    raise ValueError("Stage 8L learned-control provenance disagrees with execution")
            if (
                control.get("forge_search_unchanged") is not True
                or control.get("forge_phase_deferral_unchanged") is not True
            ):
                raise ValueError("Stage 8L learned action changed Forge before return boundary")

        safe = public_input(state, capture["candidates"])
        actor = state.get("acting_player")
        if type(actor) is not int or actor not in (0, 1):
            raise ValueError("Stage 8L requires zero-based Forge actor ID")
        game_result = 0.5 if winner is None else float(actor == winner)
        row = {
            "schema_version": SCHEMA_VERSION,
            "trajectory_id": f"{path.stem}:decision-{idx}",
            "source_log": path.name,
            "source_log_sha256": source_sha256,
            "trajectory_step": ret.get("priority_return_index"),
            "model_input": safe,
            "behavior_action": executed,
            "behavior_source": behavior_source,
            "behavior_action_targeted": _is_targeted(executed),
            "labels": {
                "game_result": game_result,
                "terminal_action_outcome": terminal.get("outcome"),
            },
            "audit": {
                "run_seed": capture.get("run_seed"),
                "decision_index": idx,
                "matchup_id": capture.get("matchup_id"),
                "acting_player": actor,
                "acting_player_name": state.get("acting_player_name"),
                "turn": state.get("turn"),
                "phase": state.get("phase"),
                "forge_proposed_action": forge_action,
                "forge_referee": True,
                "return_boundary_verified": True,
                "controller_acceptance_verified": True,
                "terminal_binding_verified": True,
                "hidden_information_fields": 0,
                "promotion_allowed": False,
            },
        }
        rows.append(validate_trajectory_row(row))
    return rows


def collect_logs(paths: list[Path], exploration_seeds=EXPLORATION_SEEDS) -> tuple[list[dict], dict]:
    rows = []
    seen_ids = set()
    seen_logs = set()
    seed_games = Counter()
    for path in paths:
        if path.name in seen_logs:
            raise ValueError("duplicate Stage 8L source log")
        seen_logs.add(path.name)
        log_rows = collect_log(path)
        seeds = {row["audit"]["run_seed"] for row in log_rows}
        if len(seeds) != 1:
            raise ValueError("Stage 8L source log crosses seed families")
        seed = next(iter(seeds))
        if seed not in exploration_seeds:
            raise ValueError(f"unexpected Stage 8L seed family: {seed}")
        seed_games[seed] += 1
        for row in log_rows:
            if row["trajectory_id"] in seen_ids:
                raise ValueError("duplicate Stage 8L trajectory ID")
            seen_ids.add(row["trajectory_id"])
            rows.append(row)

    missing = sorted(set(exploration_seeds) - set(seed_games))
    if missing:
        raise ValueError(f"missing Stage 8L exploration seed families: {missing}")

    behavior = Counter(row["behavior_source"] for row in rows)
    outcomes = Counter(str(row["labels"]["game_result"]) for row in rows)
    terminal = Counter(row["labels"]["terminal_action_outcome"] for row in rows)
    targeted = sum(row["behavior_action_targeted"] for row in rows)
    manifest = {
        "schema_version": MANIFEST_VERSION,
        "mode": "public-only-outcome-trajectory-collection",
        "exploration_seed_families": list(exploration_seeds),
        "reserved_untouched_evaluation_seed_families": list(RESERVED_EVALUATION_SEEDS),
        "games": len(paths),
        "trajectory_rows": len(rows),
        "behavior_source_counts": dict(sorted(behavior.items())),
        "game_result_label_counts": dict(sorted(outcomes.items())),
        "terminal_outcome_counts": dict(sorted(terminal.items())),
        "targeted_behavior_rows": targeted,
        "model_input_contains_search_scores": False,
        "model_input_contains_terminal_outcomes": False,
        "model_input_contains_hidden_information": False,
        "forge_referee": True,
        "training_performed": False,
        "promotion_allowed": False,
        "broader_learned_control_allowed": False,
        "next_stage": (
            "Freeze an outcome-oriented learner specification, train only on Stage 8L "
            "exploration families, and evaluate once on reserved seeds 20261032-20261033."
        ),
    }
    return rows, manifest


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("logs", nargs="+", type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--manifest", required=True, type=Path)
    args = parser.parse_args()
    rows, manifest = collect_logs(args.logs)
    args.output.write_text(
        "".join(canonical_json(row) + "\n" for row in rows), encoding="utf-8"
    )
    args.manifest.write_text(json.dumps(manifest, sort_keys=True, indent=2) + "\n")
    print(canonical_json({
        "games": manifest["games"],
        "trajectory_rows": manifest["trajectory_rows"],
        "learned_rows": manifest["behavior_source_counts"].get(
            "learned-return-boundary", 0
        ),
        "targeted_behavior_rows": manifest["targeted_behavior_rows"],
        "promotion_allowed": manifest["promotion_allowed"],
    }))


if __name__ == "__main__":
    main()
