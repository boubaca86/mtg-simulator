#!/usr/bin/env python3
"""Stage 8O: conservative public-only outcome learner on the 8L+8N corpora.

Forge remains the sole rules and legality referee. This is offline-only: the
model cannot execute an action. Only observed behavior actions have outcome
labels; counterfactual actions are never assigned invented results.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
from collections import Counter, defaultdict
from pathlib import Path
from types import MappingProxyType

from stage8d_shadow_policy import public_input
from stage8l_outcome_trajectories import validate_trajectory_row, EXPLORATION_SEEDS
from stage8n_outcome_trajectories import validate_row as validate_8n_row
from stage8n_exploration import DEVELOPMENT_SEEDS
from stage8_ranker_features import canonical_action

SCHEMA = "stage8o-conservative-outcome-v1"
REPORT = "stage8o-development-report-v1"
TRAIN_SEEDS = tuple(sorted((*EXPLORATION_SEEDS, *DEVELOPMENT_SEEDS)))
RESERVED_SEEDS = (20261032, 20261033)
CONFIG = {
    "epochs": 160,
    "learning_rate": 0.02,
    "l2": 0.05,
    "features": "fixed-small-public-action-v1",
    "objective": "observed-behavior-game-outcome-logistic",
    "weighting": "equal-total-weight-per-game",
    "validation": "leave-one-entire-seed-family-out",
    "constant_baseline": "training-game-weighted-mean",
    "candidate_ties": "preserve-all",
}
SOURCE_FILES = (
    "stage8o_outcome_policy.py", "stage8l_outcome_trajectories.py",
    "stage8n_outcome_trajectories.py", "stage8d_shadow_policy.py",
    "stage8_ranker_features.py",
)
PHASES = ("MAIN1", "MAIN2", "COMBAT", "BEGIN", "END")
ACTION_WORDS = ("attack", "block", "cast", "draw", "damage", "destroy", "land",
                "creature", "mana", "counter", "sacrifice", "exile")


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"), allow_nan=False)


def digest(value):
    return hashlib.sha256(canonical(value).encode()).hexdigest()


def source_hashes():
    root = Path(__file__).resolve().parent
    return {name: hashlib.sha256((root / name).read_bytes()).hexdigest()
            for name in SOURCE_FILES}


def validate_input(raw):
    if not isinstance(raw, dict) or set(raw) != {"public_state", "candidates"}:
        raise ValueError("Stage 8O requires exact public input envelope")
    clean = public_input(raw["public_state"], raw["candidates"])
    if clean != raw:
        raise ValueError("Stage 8O rejects extra or altered model-input fields")
    return clean


def checked_rows(early, later):
    if len(early) != 115 or len(later) != 216:
        raise ValueError("Stage 8O requires the complete pinned 115+216 rows")
    games = defaultdict(set)
    seen = set()
    output = []
    for kind, rows in (("8L", early), ("8N", later)):
        for row in rows:
            if kind == "8L":
                validate_trajectory_row(row)
            else:
                validate_8n_row(row)
            source = row.get("source_log")
            seed = row.get("audit", {}).get("corpus_seed")
            identifier = row.get("trajectory_id")
            if not isinstance(source, str) or not source or not isinstance(identifier, str):
                raise ValueError("Stage 8O row missing source/identity")
            if type(seed) is not int or seed in RESERVED_SEEDS:
                raise ValueError("Stage 8O reserved/invalid seed")
            family = EXPLORATION_SEEDS if kind == "8L" else DEVELOPMENT_SEEDS
            if seed not in family:
                raise ValueError("Stage 8O development seed family mismatch")
            key = (kind, identifier)
            if key in seen:
                raise ValueError("Stage 8O duplicate trajectory")
            seen.add(key)
            game = (kind, source)
            games[game].add(seed)
            safe = validate_input(row["model_input"])
            action = row.get("behavior_action")
            if sum(c["action_identity"] == action for c in safe["candidates"]) != 1:
                raise ValueError("Stage 8O observed action not uniquely legal")
            label = row.get("labels", {}).get("game_result")
            if type(label) not in (float, int) or label not in (0.0, 0.5, 1.0):
                raise ValueError("Stage 8O invalid outcome")
            output.append((kind, row))
    if len(games) != 24 or any(len(s) != 1 for s in games.values()):
        raise ValueError("Stage 8O incomplete or cross-seed game groups")
    by_seed = Counter(next(iter(s)) for s in games.values())
    if set(by_seed) != set(TRAIN_SEEDS) or any(v != 2 for v in by_seed.values()):
        raise ValueError("Stage 8O missing two games per seed family")
    if len({key[1] for key in games}) != len(games):
        raise ValueError("Stage 8O duplicate game source across corpora")
    return output


def number(value, scale=1.0):
    if type(value) not in (int, float) or not math.isfinite(value):
        raise ValueError("Stage 8O requires finite public numeric data")
    return max(-3.0, min(3.0, float(value) / scale))


def features(inp, candidate):
    safe = validate_input(inp)
    if candidate not in safe["candidates"]:
        raise ValueError("Stage 8O candidate is outside the public legal set")
    state = safe["public_state"]
    action = canonical_action(candidate["action_identity"], state["acting_player_name"])
    # Deliberately bounded, fixed-vocabulary features. No card IDs/names,
    # search scores, opponent-hidden identities, actor/deck identity or seeds.
    out = {
        "bias": 1.0,
        "turn": number(state["turn"], 20.0),
        "life_diff": number(state["acting_life"] - state["opponent_life"], 20.0),
        "hand_count": number(len(state["own_hand"]), 10.0),
        "own_board": number(len(state["own_battlefield"]), 10.0),
        "opp_board": number(len(state["opponent_battlefield"]), 10.0),
        "own_graveyard": number(len(state["own_graveyard"]), 10.0),
        "opp_graveyard": number(len(state["opponent_graveyard"]), 10.0),
        "opp_unknown_hand_count": number(state["opponent_unknown_hand_count"], 10.0),
        "own_library_count": number(state["own_library_count"], 60.0),
        "opp_library_count": number(state["opponent_library_count"], 60.0),
        "candidate_count": number(len(safe["candidates"]), 10.0),
        "target_count": number(len(candidate["target_public_semantics"]), 4.0),
        "targeted": float(len(candidate["target_public_semantics"]) > 0),
        "target_player": float(any("zone=player|" in x for x in candidate["target_public_semantics"])),
        "target_opponent": float(any("role=opponent|" in x for x in candidate["target_public_semantics"])),
        "target_self": float(any("role=self|" in x for x in candidate["target_public_semantics"])),
        "modes": float("modes=<none>" not in action),
        "choices": float("choices=<none>" not in action),
        "x_announced": float("x=<none>" not in action),
    }
    for phase in PHASES:
        out["phase_"+phase.lower()] = float(phase in state["phase"].upper())
    ability = action.split("|", 1)[0]
    for word in ACTION_WORDS:
        out["action_"+word] = float(bool(re.search(r"\\b"+word+r"\\b", ability)))
    if not all(math.isfinite(v) and -3 <= v <= 3 for v in out.values()):
        raise ValueError("Stage 8O invalid compact feature")
    return out


def sigmoid(z):
    return 1.0 / (1.0 + math.exp(-max(-35.0, min(35.0, z))))


def predict_one(weights, x):
    return sigmoid(sum(weights.get(k, 0.0) * v for k, v in x.items()))


def weighted_examples(records):
    counts = Counter((kind, row["source_log"]) for kind, row in records)
    for kind, row in sorted(records, key=lambda r: (r[0], r[1]["trajectory_id"])):
        candidate = next(c for c in row["model_input"]["candidates"]
                         if c["action_identity"] == row["behavior_action"])
        yield (features(row["model_input"], candidate),
               float(row["labels"]["game_result"]),
               1.0 / counts[(kind, row["source_log"])],
               (kind, row["source_log"]))


def fit(records):
    samples = list(weighted_examples(records))
    if not samples:
        raise ValueError("Stage 8O empty training fold")
    mass = sum(sample[2] for sample in samples)
    weights = {key: 0.0 for key in features(
        records[0][1]["model_input"], next(
            c for c in records[0][1]["model_input"]["candidates"]
            if c["action_identity"] == records[0][1]["behavior_action"]
        )).keys()}
    for _ in range(CONFIG["epochs"]):
        gradient = {key: 0.0 for key in weights}
        for x, label, sample_weight, _ in samples:
            error = (predict_one(weights, x) - label) * sample_weight
            for key, value in x.items():
                gradient[key] += error * value
        for key in sorted(weights):
            regularization = 0.0 if key == "bias" else CONFIG["l2"] * weights[key]
            weights[key] -= CONFIG["learning_rate"] * (gradient[key] / mass + regularization)
            if not math.isfinite(weights[key]):
                raise ValueError("Stage 8O nonfinite training weight")
    return weights


def cross_validation(records):
    model_losses = []
    prior_losses = []
    folds = []
    for holdout_seed in TRAIN_SEEDS:
        train = [(kind, row) for kind, row in records
                 if row["audit"]["corpus_seed"] != holdout_seed]
        held = [(kind, row) for kind, row in records
                if row["audit"]["corpus_seed"] == holdout_seed]
        if not train or not held:
            raise ValueError("Stage 8O empty grouped fold")
        train_games = {(k,r["source_log"]) for k,r in train}
        held_games = {(k,r["source_log"]) for k,r in held}
        if train_games & held_games or len(held_games) != 2:
            raise ValueError("Stage 8O game-group leakage")
        weights = fit(train)
        examples_train = list(weighted_examples(train))
        prior = sum(y*w for _,y,w,_ in examples_train) / sum(w for _,_,w,_ in examples_train)
        examples_held = list(weighted_examples(held))
        fold_model = []
        fold_prior = []
        for x, y, game_weight, _ in examples_held:
            fold_model.append((predict_one(weights, x), y, game_weight))
            fold_prior.append((prior, y, game_weight))
        model_losses.extend(fold_model)
        prior_losses.extend(fold_prior)
        folds.append({
            "holdout_seed": holdout_seed,
            "train_games": len(train_games),
            "held_games": len(held_games),
            "model": metrics(fold_model),
            "baseline": metrics(fold_prior),
        })
    final_model = metrics(model_losses)
    final_baseline = metrics(prior_losses)
    return {
        "method": CONFIG["validation"],
        "folds": folds,
        "model": final_model,
        "constant_baseline": final_baseline,
        "development_gate_passed": (
            final_model["logloss"] < final_baseline["logloss"] - 1e-6
            and final_model["brier"] < final_baseline["brier"] - 1e-6
        ),
        "strength_claim_allowed": False,
    }


def metrics(items):
    mass = sum(w for _,_,w in items)
    if mass <= 0:
        raise ValueError("Stage 8O empty metrics")
    return {
        "logloss": sum((-y*math.log(max(1e-9, min(1-1e-9,p)))
                        -(1-y)*math.log(max(1e-9, min(1-1e-9,1-p))))*w
                       for p,y,w in items) / mass,
        "brier": sum((p-y)**2*w for p,y,w in items) / mass,
    }


def export(early: Path, later: Path):
    early_bytes, later_bytes = early.read_bytes(), later.read_bytes()
    early_rows = [json.loads(line) for line in early_bytes.splitlines() if line.strip()]
    later_rows = [json.loads(line) for line in later_bytes.splitlines() if line.strip()]
    records = checked_rows(early_rows, later_rows)
    cv = cross_validation(records)
    weights = fit(records)
    payload = {
        "schema_version": SCHEMA,
        "mode": "offline-shadow-only",
        "promotion_allowed": False,
        "broader_learned_control_allowed": False,
        "training_performed": True,
        "training_seeds": list(TRAIN_SEEDS),
        "reserved_untouched_evaluation_seeds": list(RESERVED_SEEDS),
        "training_rows": len(records),
        "training_games": 24,
        "corpus_sha256": {
            "stage8l": hashlib.sha256(early_bytes).hexdigest(),
            "stage8n": hashlib.sha256(later_bytes).hexdigest(),
        },
        "source_sha256": source_hashes(),
        "config": CONFIG,
        "weights": weights,
    }
    payload["model_id"] = digest(payload)
    report = {
        "schema_version": REPORT,
        "model_id": payload["model_id"],
        "training_rows": len(records),
        "training_games": 24,
        "cross_validation": cv,
        "promotion_allowed": False,
        "broader_learned_control_allowed": False,
        "reserved_evaluation_opened": False,
        "next_stage_authorized": "one-shot-reserved-evaluation" if cv["development_gate_passed"] else "development-research-only",
    }
    return payload, report


def load(path):
    payload = json.loads(path.read_text())
    fingerprint = payload.pop("model_id", None)
    if fingerprint != digest(payload):
        raise ValueError("Stage 8O checkpoint hash mismatch")
    if (payload.get("schema_version") != SCHEMA
        or payload.get("mode") != "offline-shadow-only"
        or payload.get("config") != CONFIG
        or payload.get("training_seeds") != list(TRAIN_SEEDS)
        or payload.get("reserved_untouched_evaluation_seeds") != list(RESERVED_SEEDS)
        or payload.get("training_rows") != 331
        or payload.get("training_games") != 24
        or payload.get("source_sha256") != source_hashes()
        or payload.get("promotion_allowed") is not False
        or payload.get("broader_learned_control_allowed") is not False):
        raise ValueError("Stage 8O checkpoint provenance mismatch")
    weights = payload.get("weights")
    expected = {
        "bias", "turn", "life_diff", "hand_count", "own_board", "opp_board",
        "own_graveyard", "opp_graveyard", "opp_unknown_hand_count",
        "own_library_count", "opp_library_count", "candidate_count",
        "target_count", "targeted", "target_player", "target_opponent",
        "target_self", "modes", "choices", "x_announced",
    } | {"phase_"+p.lower() for p in PHASES} | {"action_"+w for w in ACTION_WORDS}
    if not isinstance(weights, dict) or set(weights) != expected:
        raise ValueError("Stage 8O incomplete checkpoint weights")
    if not all(isinstance(k,str) and type(v) in (int,float) and math.isfinite(v)
               for k,v in weights.items()):
        raise ValueError("Stage 8O nonfinite weights")
    return {"model_id": fingerprint, "weights": MappingProxyType(weights)}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("stage8l", type=Path)
    parser.add_argument("stage8n", type=Path)
    parser.add_argument("--checkpoint", required=True, type=Path)
    parser.add_argument("--report", required=True, type=Path)
    args = parser.parse_args()
    checkpoint, report = export(args.stage8l, args.stage8n)
    args.checkpoint.write_text(canonical(checkpoint)+"\n")
    args.report.write_text(json.dumps(report, sort_keys=True, indent=2)+"\n")
    print(canonical({"model_id": checkpoint["model_id"],
                     "development_gate_passed": report["cross_validation"]["development_gate_passed"],
                     "promotion_allowed": False}))


if __name__ == "__main__":
    main()
