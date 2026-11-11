#!/usr/bin/env python3
"""Stage 8R: public-only, offline, grouped outcome diagnostic.

No action dispatch, no gameplay, no seed consumption and no promotion. This
learner operates on the prior immutable 8L/8N development artifacts. Forge is
the only rules referee. The observational game result is NOT a counterfactual
action-value label for the other legal candidates.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from collections import Counter
from pathlib import Path

import stage8o_outcome_policy as old
from stage8q_target_effect_features import VERSION as FEATURE_VERSION
from stage8q_target_effect_features import features as target_features

SCHEMA = "stage8r-development-outcome-v1"
REPORT = "stage8r-grouped-outcome-comparison-v1"
CONSUMED_RESERVED_SEEDS = (20261032, 20261033)
# These are only proposed identities, NOT authorization or a claim that the
# GitHub run history has already proven the seeds unused.
PROPOSED_FRESH_DEV_SEEDS = tuple(range(20261042, 20261050))
PROPOSED_FRESH_HOLDOUT_SEEDS = tuple(range(20261050, 20261058))
CONFIG = {
    "epochs": 160,
    "learning_rate": 0.02,
    "l2": 0.05,
    "objective": "observed-behavior-game-outcome-logistic",
    "weighting": "equal-total-weight-per-game",
    "validation": "leave-one-entire-seed-family-out",
    "comparator": "frozen-stage8o-method-same-complete-seed-folds",
    "feature_family": FEATURE_VERSION,
    "selection_threshold": 1e-6,
}
SOURCES = (
    "stage8r_outcome_diagnostic.py",
    "stage8q_target_effect_features.py",
    "stage8o_outcome_policy.py",
    "stage8c_target_features.py",
    "stage8d_shadow_policy.py",
    "stage8_ranker_features.py",
)


def _sha(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def source_hashes() -> dict:
    base = Path(__file__).resolve().parent
    return {path: _sha((base / path).read_bytes()) for path in SOURCES}


def read_training(early: Path, later: Path):
    """Fail closed on forbidden seeds, malformed provenance and partial games."""
    blobs = [early.read_bytes(), later.read_bytes()]
    sets = [[json.loads(line) for line in blob.splitlines() if line.strip()]
            for blob in blobs]
    records = old.checked_rows(*sets)  # 331 rows, 24 distinct complete games.
    families = {row["audit"]["corpus_seed"] for _, row in records}
    if families != set(old.TRAIN_SEEDS):
        raise ValueError("Stage 8R incomplete or forbidden training seed families")
    if families & (set(CONSUMED_RESERVED_SEEDS)
                   | set(PROPOSED_FRESH_DEV_SEEDS)
                   | set(PROPOSED_FRESH_HOLDOUT_SEEDS)):
        raise ValueError("Stage 8R training data overlaps quarantined/future seeds")
    return records, {"stage8l": _sha(blobs[0]), "stage8n": _sha(blobs[1])}


def observations(records):
    """One observation per executed public legal action, one weight per game."""
    counts = Counter((kind, row["source_log"]) for kind, row in records)
    data = []
    for kind, row in sorted(records, key=lambda p: (p[0], p[1]["trajectory_id"])):
        options = row["model_input"]["candidates"]
        behavior = row["behavior_action"]
        choices = [c for c in options if c["action_identity"] == behavior]
        if len(choices) != 1:
            raise ValueError("Stage 8R action was not uniquely present in Forge legal set")
        x = target_features(row["model_input"], choices[0])
        if not all(math.isfinite(value) for value in x.values()):
            raise ValueError("nonfinite Stage 8R features")
        data.append({
            "features": x,
            "outcome": float(row["labels"]["game_result"]),
            "weight": 1.0 / counts[(kind, row["source_log"])],
            "family": row["audit"]["corpus_seed"],
            "game": (kind, row["source_log"]),
        })
    if len(data) != 331 or len({r["game"] for r in data}) != 24:
        raise ValueError("Stage 8R incomplete observational corpus")
    keys = set(data[0]["features"])
    if not all(set(r["features"]) == keys for r in data):
        raise ValueError("Stage 8R feature vocabulary is not fixed")
    return data, sorted(keys)


def fit(rows, feature_names):
    if not rows:
        raise ValueError("empty Stage 8R training fold")
    mass = sum(r["weight"] for r in rows)
    if not mass > 0:
        raise ValueError("invalid Stage 8R training weights")
    weights = dict.fromkeys(feature_names, 0.0)
    for _ in range(CONFIG["epochs"]):
        gradient = dict.fromkeys(feature_names, 0.0)
        for sample in rows:
            error = (old.predict_one(weights, sample["features"]) - sample["outcome"]) * sample["weight"]
            for key, value in sample["features"].items():
                gradient[key] += error * value
        for key in feature_names:
            regularizer = 0.0 if key == "bias" else CONFIG["l2"] * weights[key]
            weights[key] -= CONFIG["learning_rate"] * (gradient[key] / mass + regularizer)
            if not math.isfinite(weights[key]):
                raise ValueError("nonfinite Stage 8R model weight")
    return weights


def grouped_comparison(records, observations_data, feature_names):
    """Compare equal complete-seed-family folds, never same-game train/test."""
    old_cv = old.cross_validation(records)
    if [f["holdout_seed"] for f in old_cv["folds"]] != list(old.TRAIN_SEEDS):
        raise ValueError("Stage 8O comparator fold mismatch")
    scored, folds = [], []
    for held_seed in old.TRAIN_SEEDS:
        train = [r for r in observations_data if r["family"] != held_seed]
        held = [r for r in observations_data if r["family"] == held_seed]
        train_games = {r["game"] for r in train}
        held_games = {r["game"] for r in held}
        if (train_games & held_games or len(held_games) != 2
                or len(train_games) != 22
                or len(held) == 0):
            raise ValueError("Stage 8R game or family leakage")
        model = fit(train, feature_names)
        fold_scores = [(old.predict_one(model, r["features"]), r["outcome"], r["weight"]) for r in held]
        scored.extend(fold_scores)
        folds.append({
            "held_seed": held_seed,
            "held_games": len(held_games),
            "train_games": len(train_games),
            "model": old.metrics(fold_scores),
        })
    new_scores = old.metrics(scored)
    old_scores = old_cv["model"]
    constant_scores = old_cv["constant_baseline"]
    margin = CONFIG["selection_threshold"]
    beats_old = (new_scores["logloss"] < old_scores["logloss"] - margin
                 and new_scores["brier"] < old_scores["brier"] - margin)
    beats_constant = (new_scores["logloss"] < constant_scores["logloss"] - margin
                      and new_scores["brier"] < constant_scores["brier"] - margin)
    return {
        "model": new_scores,
        "stage8o_model": old_scores,
        "constant_baseline": constant_scores,
        "folds": folds,
        "stage8q_representation_improved_both_metrics": beats_old and beats_constant,
        "delta_logloss_vs_stage8o": new_scores["logloss"] - old_scores["logloss"],
        "delta_brier_vs_stage8o": new_scores["brier"] - old_scores["brier"],
        "strength_claim_allowed": False,
    }


def export(early: Path, later: Path):
    records, corpus = read_training(early, later)
    samples, names = observations(records)
    cv = grouped_comparison(records, samples, names)
    model = {
        "schema_version": SCHEMA,
        "mode": "offline-development-only",
        "config": CONFIG,
        "training_rows": 331,
        "training_games": 24,
        "training_seeds": list(old.TRAIN_SEEDS),
        "consumed_reserved_seeds": list(CONSUMED_RESERVED_SEEDS),
        "future_proposed_development_seeds": list(PROPOSED_FRESH_DEV_SEEDS),
        "future_proposed_holdout_seeds": list(PROPOSED_FRESH_HOLDOUT_SEEDS),
        "source_sha256": source_hashes(),
        "corpus_sha256": corpus,
        "feature_names": names,
        "weights": fit(samples, names),
        "promotion_allowed": False,
        "broader_learned_control_allowed": False,
        "strength_claim_allowed": False,
    }
    model["model_id"] = old.digest(model)
    report = {
        "schema_version": REPORT,
        "model_id": model["model_id"],
        "training_rows": model["training_rows"],
        "training_games": model["training_games"],
        "grouped_development_comparison": cv,
        "proposed_fresh_seed_families_unverified": True,
        "new_gameplay_executed": False,
        "reserved_gameplay_replayed": False,
        "promotion_allowed": False,
        "broader_learned_control_allowed": False,
        "strength_claim_allowed": False,
    }
    return model, report


def load(checkpoint: Path):
    payload = json.loads(checkpoint.read_text())
    fingerprint = payload.pop("model_id", None)
    if fingerprint != old.digest(payload):
        raise ValueError("Stage 8R checkpoint hash mismatch")
    if (payload.get("schema_version") != SCHEMA
            or payload.get("mode") != "offline-development-only"
            or payload.get("config") != CONFIG
            or payload.get("training_rows") != 331
            or payload.get("training_games") != 24
            or payload.get("training_seeds") != list(old.TRAIN_SEEDS)
            or payload.get("consumed_reserved_seeds") != list(CONSUMED_RESERVED_SEEDS)
            or payload.get("future_proposed_development_seeds") != list(PROPOSED_FRESH_DEV_SEEDS)
            or payload.get("future_proposed_holdout_seeds") != list(PROPOSED_FRESH_HOLDOUT_SEEDS)
            or payload.get("source_sha256") != source_hashes()
            or payload.get("promotion_allowed") is not False
            or payload.get("broader_learned_control_allowed") is not False
            or payload.get("strength_claim_allowed") is not False):
        raise ValueError("Stage 8R model provenance mismatch")
    names = payload.get("feature_names")
    weights = payload.get("weights")
    if (not isinstance(names, list) or not names or names != sorted(set(names))
            or not isinstance(weights, dict) or set(weights) != set(names)
            or any(type(v) not in (float, int) or not math.isfinite(v)
                   for v in weights.values())):
        raise ValueError("Stage 8R malformed weights")
    return {"model_id": fingerprint, "weights": weights}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("stage8l", type=Path)
    parser.add_argument("stage8n", type=Path)
    parser.add_argument("--checkpoint", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    checkpoint, report = export(args.stage8l, args.stage8n)
    args.checkpoint.write_text(old.canonical(checkpoint) + "\n")
    args.report.write_text(old.canonical(report) + "\n")
    cv = report["grouped_development_comparison"]
    print(old.canonical({
        "model_id": checkpoint["model_id"],
        "comparison": cv,
        "promotion_allowed": False,
    }))


if __name__ == "__main__":
    main()
