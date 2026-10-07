#!/usr/bin/env python3
"""Stage 8M: frozen public-only outcome policy trained from Stage 8L trajectories.

The learner observes only the Stage 8L model_input allowlist. The executed
behavior action and eventual game result are training labels, never inference
features. Forge remains the sole rules referee and executor.

Stage 8M is a training/reproducibility stage, not a strength claim. The frozen
model has no gameplay-control interface and must not consume the reserved
20261032-20261033 evaluation families.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from types import MappingProxyType

from stage8c_target_features import features as stage8c_features
from stage8l_outcome_trajectories import (
    EXPLORATION_SEEDS,
    RESERVED_EVALUATION_SEEDS,
    validate_model_input,
    validate_trajectory_row,
)

MODEL_SCHEMA = "stage8m-outcome-policy-v1"
REPORT_SCHEMA = "stage8m-outcome-training-report-v1"
TRAINING_SEEDS = tuple(EXPLORATION_SEEDS)
RESERVED_SEEDS = tuple(RESERVED_EVALUATION_SEEDS)
CONFIG = {
    "objective": "game-outcome-logistic",
    "representation": "stage8c-public-target-semantics",
    "epochs": 240,
    "learning_rate": 0.03,
    "l2": 0.001,
    "game_weighting": "equal-total-weight-per-source-log",
    "validation": "leave-one-corpus-seed-family-out",
    "selection": "all-max-with-tolerance",
}
SOURCE_FILES = (
    "stage8m_outcome_policy.py",
    "stage8c_target_features.py",
    "stage8_ranker_features.py",
    "stage8l_outcome_trajectories.py",
)


def canonical_json(value) -> str:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), allow_nan=False)


def fingerprint(value) -> str:
    return hashlib.sha256(canonical_json(value).encode("utf-8")).hexdigest()


def source_hashes() -> dict[str, str]:
    here = Path(__file__).resolve().parent
    return {
        name: hashlib.sha256((here / name).read_bytes()).hexdigest()
        for name in SOURCE_FILES
    }


def sigmoid(value: float) -> float:
    value = max(-40.0, min(40.0, value))
    return 1.0 / (1.0 + math.exp(-value))


def sparse_dot(weights: dict | MappingProxyType, vector: dict) -> float:
    return sum(weights.get(key, 0.0) * value for key, value in vector.items())


def behavior_candidate(row: dict) -> dict:
    action = row["behavior_action"]
    matches = [
        candidate
        for candidate in row["model_input"]["candidates"]
        if candidate.get("action_identity") == action
    ]
    if len(matches) != 1:
        raise ValueError("Stage 8M behavior action must match exactly one legal candidate")
    return matches[0]


def outcome_features(model_input: dict, candidate: dict) -> dict:
    safe = validate_model_input(model_input)
    if candidate not in safe["candidates"]:
        raise ValueError("Stage 8M candidate is outside the validated public candidate set")
    vector = stage8c_features({"public_state": safe["public_state"]}, candidate)
    if not vector or not all(
        isinstance(key, tuple)
        and all(isinstance(part, str) for part in key)
        and isinstance(value, (int, float))
        and not isinstance(value, bool)
        and math.isfinite(value)
        for key, value in vector.items()
    ):
        raise ValueError("Stage 8M produced an invalid public feature vector")
    return vector


def validate_rows(rows: list[dict]) -> list[dict]:
    if not rows:
        raise ValueError("empty Stage 8M training corpus")
    seen_ids = set()
    game_seed = {}
    seed_games = defaultdict(set)
    for row in rows:
        validate_trajectory_row(row)
        trajectory_id = row.get("trajectory_id")
        source_log = row.get("source_log")
        if not isinstance(trajectory_id, str) or not trajectory_id or trajectory_id in seen_ids:
            raise ValueError("missing or duplicate Stage 8M trajectory ID")
        if not isinstance(source_log, str) or not source_log:
            raise ValueError("Stage 8M requires source-log game groups")
        seen_ids.add(trajectory_id)

        seed = row.get("audit", {}).get("corpus_seed")
        if type(seed) is not int:
            raise ValueError("Stage 8M requires integer corpus_seed provenance")
        if seed in RESERVED_SEEDS:
            raise ValueError("reserved Stage 8M evaluation seed entered training")
        if seed not in TRAINING_SEEDS:
            raise ValueError("unexpected Stage 8M training seed family")
        if source_log in game_seed and game_seed[source_log] != seed:
            raise ValueError("one Stage 8M game crosses corpus seed families")
        game_seed[source_log] = seed
        seed_games[seed].add(source_log)

        outcome = row.get("labels", {}).get("game_result")
        if outcome not in (0.0, 0.5, 1.0):
            raise ValueError("Stage 8M requires a valid game-result label")
        outcome_features(row["model_input"], behavior_candidate(row))

    if set(seed_games) != set(TRAINING_SEEDS):
        raise ValueError("Stage 8M training corpus does not cover every exploration seed")
    if any(len(seed_games[seed]) != 2 for seed in TRAINING_SEEDS):
        raise ValueError("Stage 8M requires exactly two source games per exploration seed")
    return rows


def game_weights(rows: list[dict]) -> dict[str, float]:
    counts = Counter(row["source_log"] for row in rows)
    if not counts:
        raise ValueError("empty Stage 8M game weighting")
    return {game: 1.0 / count for game, count in counts.items()}


def examples(rows: list[dict]) -> list[tuple[dict, float, float, str]]:
    weights = game_weights(rows)
    out = []
    for row in sorted(rows, key=lambda value: value["trajectory_id"]):
        vector = outcome_features(row["model_input"], behavior_candidate(row))
        out.append((
            vector,
            float(row["labels"]["game_result"]),
            weights[row["source_log"]],
            row["source_log"],
        ))
    return out


def fit(rows: list[dict], config: dict = CONFIG) -> dict[tuple[str, ...], float]:
    training = examples(rows)
    total_weight = sum(weight for _, _, weight, _ in training)
    if total_weight <= 0:
        raise ValueError("invalid Stage 8M training weight")
    weights: dict[tuple[str, ...], float] = {}

    for _ in range(int(config["epochs"])):
        gradient = defaultdict(float)
        for vector, target, sample_weight, _ in training:
            prediction = sigmoid(sparse_dot(weights, vector))
            error = (prediction - target) * sample_weight
            for key, value in vector.items():
                gradient[key] += error * value

        keys = sorted(set(weights) | set(gradient))
        updated = {}
        for key in keys:
            value = weights.get(key, 0.0)
            grad = gradient.get(key, 0.0) / total_weight
            grad += float(config["l2"]) * value
            value -= float(config["learning_rate"]) * grad
            if not math.isfinite(value):
                raise ValueError("nonfinite Stage 8M model weight")
            if value:
                updated[key] = value
        weights = updated

    if not weights:
        raise ValueError("Stage 8M training produced an empty model")
    return weights


def clipped_logloss(probability: float, target: float) -> float:
    p = min(1.0 - 1e-9, max(1e-9, probability))
    return -(target * math.log(p) + (1.0 - target) * math.log(1.0 - p))


def weighted_metrics(records: list[dict]) -> dict:
    if not records:
        raise ValueError("empty Stage 8M evaluation records")
    by_game = Counter(record["source_log"] for record in records)
    weighted = []
    for record in records:
        weight = 1.0 / by_game[record["source_log"]]
        weighted.append((record, weight))
    total = sum(weight for _, weight in weighted)
    return {
        "rows": len(records),
        "games": len(by_game),
        "logloss": sum(
            clipped_logloss(record["probability"], record["target"]) * weight
            for record, weight in weighted
        ) / total,
        "brier": sum(
            (record["probability"] - record["target"]) ** 2 * weight
            for record, weight in weighted
        ) / total,
        "unique_recommendation_rate": sum(
            bool(record["unique_recommendation"]) * weight
            for record, weight in weighted
        ) / total,
        "behavior_agreement_rate": sum(
            bool(record["behavior_agreed"]) * weight
            for record, weight in weighted
        ) / total,
    }


def baseline_probability(rows: list[dict]) -> float:
    weights = game_weights(rows)
    total = 0.0
    mass = 0.0
    for row in rows:
        weight = weights[row["source_log"]]
        total += float(row["labels"]["game_result"]) * weight
        mass += weight
    if mass <= 0:
        raise ValueError("invalid Stage 8M baseline mass")
    return total / mass


def score_input(weights: dict | MappingProxyType, model_input: dict) -> dict:
    safe = validate_model_input(model_input)
    scored = []
    for candidate in safe["candidates"]:
        probability = sigmoid(sparse_dot(weights, outcome_features(safe, candidate)))
        if not math.isfinite(probability):
            raise ValueError("nonfinite Stage 8M candidate probability")
        scored.append({
            "action_identity": candidate["action_identity"],
            "outcome_probability": probability,
        })
    scored.sort(key=lambda item: item["action_identity"])
    high = max(item["outcome_probability"] for item in scored)
    top = [
        item["action_identity"]
        for item in scored
        if math.isclose(item["outcome_probability"], high, rel_tol=1e-12, abs_tol=1e-12)
    ]
    return {
        "top_actions": top,
        "recommendation": top[0] if len(top) == 1 else None,
        "candidate_scores": scored,
    }


def cross_validate(rows: list[dict]) -> dict:
    folds = []
    all_model_records = []
    all_baseline_records = []
    for holdout_seed in TRAINING_SEEDS:
        train = [row for row in rows if row["audit"]["corpus_seed"] != holdout_seed]
        holdout = [row for row in rows if row["audit"]["corpus_seed"] == holdout_seed]
        if not train or not holdout:
            raise ValueError("Stage 8M cross-validation fold is empty")
        if {row["source_log"] for row in train} & {row["source_log"] for row in holdout}:
            raise ValueError("Stage 8M same-game leakage across a fold")

        weights = fit(train)
        baseline = baseline_probability(train)
        model_records = []
        baseline_records = []
        for row in holdout:
            scored = score_input(weights, row["model_input"])
            probability = next(
                item["outcome_probability"]
                for item in scored["candidate_scores"]
                if item["action_identity"] == row["behavior_action"]
            )
            record = {
                "source_log": row["source_log"],
                "target": float(row["labels"]["game_result"]),
                "probability": probability,
                "unique_recommendation": scored["recommendation"] is not None,
                "behavior_agreed": row["behavior_action"] in scored["top_actions"],
            }
            model_records.append(record)
            baseline_records.append({
                **record,
                "probability": baseline,
                "unique_recommendation": False,
                "behavior_agreed": False,
            })

        fold = {
            "holdout_seed": holdout_seed,
            "train_seeds": [seed for seed in TRAINING_SEEDS if seed != holdout_seed],
            "train_rows": len(train),
            "holdout_rows": len(holdout),
            "model": weighted_metrics(model_records),
            "constant_baseline": weighted_metrics(baseline_records),
        }
        folds.append(fold)
        all_model_records.extend(model_records)
        all_baseline_records.extend(baseline_records)

    return {
        "method": CONFIG["validation"],
        "folds": folds,
        "model": weighted_metrics(all_model_records),
        "constant_baseline": weighted_metrics(all_baseline_records),
        "strength_claim_allowed": False,
    }


@dataclass(frozen=True)
class OutcomePolicy:
    model_id: str
    dataset_sha256: str
    weights: MappingProxyType

    def predict(self, model_input: dict) -> dict:
        scored = score_input(self.weights, model_input)
        return {
            "schema_version": "stage8m-outcome-recommendation-v1",
            "model_id": self.model_id,
            "mode": "shadow-only-outcome",
            "promotion_allowed": False,
            "broader_learned_control_allowed": False,
            **scored,
        }


def export_checkpoint(dataset: Path) -> tuple[dict, dict]:
    raw = dataset.read_bytes()
    dataset_sha256 = hashlib.sha256(raw).hexdigest()
    rows = [json.loads(line) for line in raw.splitlines() if line.strip()]
    validate_rows(rows)
    weights = fit(rows)
    cv = cross_validate(rows)
    payload = {
        "schema_version": MODEL_SCHEMA,
        "mode": "shadow-only-outcome",
        "promotion_allowed": False,
        "broader_learned_control_allowed": False,
        "training_performed": True,
        "training_seeds": list(TRAINING_SEEDS),
        "reserved_untouched_evaluation_seeds": list(RESERVED_SEEDS),
        "training_rows": len(rows),
        "training_games": len({row["source_log"] for row in rows}),
        "dataset_sha256": dataset_sha256,
        "config": CONFIG,
        "source_sha256": source_hashes(),
        "weights": [[list(key), value] for key, value in sorted(weights.items())],
    }
    payload["model_id"] = fingerprint(payload)
    report = {
        "schema_version": REPORT_SCHEMA,
        "model_id": payload["model_id"],
        "dataset_sha256": dataset_sha256,
        "training_rows": payload["training_rows"],
        "training_games": payload["training_games"],
        "training_seeds": list(TRAINING_SEEDS),
        "reserved_untouched_evaluation_seeds": list(RESERVED_SEEDS),
        "cross_validation": cv,
        "training_performed": True,
        "promotion_allowed": False,
        "broader_learned_control_allowed": False,
        "forge_referee_required": True,
        "next_stage": (
            "Freeze this model ID before opening reserved seeds 20261032-20261033; "
            "then evaluate with Forge still refereeing every legal action."
        ),
    }
    return payload, report


def load_checkpoint(path: Path) -> OutcomePolicy:
    payload = json.loads(path.read_text())
    model_id = payload.pop("model_id", None)
    if fingerprint(payload) != model_id:
        raise ValueError("Stage 8M checkpoint content hash mismatch")
    if (
        payload.get("schema_version") != MODEL_SCHEMA
        or payload.get("mode") != "shadow-only-outcome"
        or payload.get("promotion_allowed") is not False
        or payload.get("broader_learned_control_allowed") is not False
        or payload.get("training_performed") is not True
        or payload.get("training_seeds") != list(TRAINING_SEEDS)
        or payload.get("reserved_untouched_evaluation_seeds") != list(RESERVED_SEEDS)
        or payload.get("config") != CONFIG
        or payload.get("source_sha256") != source_hashes()
    ):
        raise ValueError("Stage 8M checkpoint provenance mismatch")
    if type(payload.get("training_rows")) is not int or payload["training_rows"] <= 0:
        raise ValueError("invalid Stage 8M training row count")
    if payload.get("training_games") != 8:
        raise ValueError("Stage 8M checkpoint requires eight training games")
    dataset_sha256 = payload.get("dataset_sha256")
    if not isinstance(dataset_sha256, str) or len(dataset_sha256) != 64:
        raise ValueError("invalid Stage 8M dataset digest")

    weights = {}
    for key, value in payload.get("weights", []):
        if (
            not isinstance(key, list)
            or not key
            or not all(isinstance(part, str) for part in key)
            or type(value) not in (int, float)
            or not math.isfinite(value)
        ):
            raise ValueError("invalid Stage 8M checkpoint weight")
        key_tuple = tuple(key)
        if key_tuple in weights:
            raise ValueError("duplicate Stage 8M checkpoint weight")
        weights[key_tuple] = float(value)
    if not weights:
        raise ValueError("empty Stage 8M checkpoint")
    return OutcomePolicy(model_id, dataset_sha256, MappingProxyType(weights))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)

    export = sub.add_parser("export")
    export.add_argument("dataset", type=Path)
    export.add_argument("--output", required=True, type=Path)
    export.add_argument("--report", required=True, type=Path)

    score = sub.add_parser("score")
    score.add_argument("checkpoint", type=Path)
    score.add_argument("model_input", type=Path)

    args = parser.parse_args()
    if args.command == "export":
        checkpoint, report = export_checkpoint(args.dataset)
        args.output.write_text(canonical_json(checkpoint) + "\n")
        args.report.write_text(json.dumps(report, sort_keys=True, indent=2) + "\n")
        print(canonical_json({
            "model_id": checkpoint["model_id"],
            "training_rows": checkpoint["training_rows"],
            "training_games": checkpoint["training_games"],
            "promotion_allowed": checkpoint["promotion_allowed"],
        }))
    else:
        policy = load_checkpoint(args.checkpoint)
        model_input = json.loads(args.model_input.read_text())
        print(canonical_json(policy.predict(model_input)))


if __name__ == "__main__":
    main()
