#!/usr/bin/env python3
"""Predeclared Stage 8C fresh-seed replication evaluator.

Train the frozen Stage 8B public-semantics baseline and Stage 8C typed-target
representation once on the pinned development corpus, then evaluate only the
untouched seed families 20261012-20261015. No learned model controls Forge.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from collections import defaultdict
from pathlib import Path

import stage8_action_ranker as base
from stage8_ranker_features import features as public_features
from stage8c_target_features import TARGET_SEMANTICS_VERSION, features as target_features

DEV_SEEDS = {20261004, 20261005, 20261006, 20261007}
FRESH_SEEDS = {20261012, 20261013, 20261014, 20261015}
PRIMARY_MIN_REGRET_IMPROVEMENT = 0.01
PRIMARY_MIN_FAMILIES_BETTER = 3
PRIMARY_MIN_TOP1_DELTA = -0.02


def load(path: Path):
    raw = path.read_bytes()
    rows = [json.loads(x) for x in raw.splitlines() if x.strip()]
    base.validate_rows(rows)
    return raw, rows


def require_target_v2(rows):
    for row in rows:
        for candidate in row["candidates"]:
            # Calling the feature extractor is the fail-closed schema check.
            target_features(row, candidate)
            if candidate.get("target_semantics_version") != TARGET_SEMANTICS_VERSION:
                raise ValueError("Stage 8C requires typed target semantics v2")


def train_weights(rows, feature_fn):
    vectors = {
        r["decision_id"]: [feature_fn(r, c) for c in r["candidates"]]
        for r in rows
    }
    weights = base.train(
        rows,
        vectors,
        epochs=base.CONFIG["epochs"],
        lr=base.CONFIG["learning_rate"],
    )
    return weights


def evaluate(rows, weights, feature_fn):
    records = []
    for row in rows:
        predictions = [base.dot(weights, feature_fn(row, c)) for c in row["candidates"]]
        records.append(base.decision_metrics(row, predictions))
    return records


def game_seed_macro(records, rows):
    seed_for = {r["decision_id"]: r["corpus_seed"] for r in rows}
    grouped = defaultdict(list)
    for record in records:
        grouped[seed_for[record["decision_id"]]].append(record)

    per_seed = {}
    for seed in sorted(FRESH_SEEDS):
        rs = grouped.get(seed, [])
        if not rs:
            raise ValueError(f"fresh family {seed} has no rankable decisions")
        aggregate = base.aggregate(rs)
        if aggregate["games"] < 1:
            raise ValueError(f"fresh family {seed} has no rankable games")
        per_seed[str(seed)] = {
            "games": aggregate["games"],
            "game_macro": aggregate["game_macro"],
            "decision_weighted": {
                key: aggregate[key]
                for key in ("top1_accuracy", "normalized_regret", "pairwise_accuracy")
            },
        }
    return per_seed


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("development", type=Path)
    ap.add_argument("fresh", type=Path)
    ap.add_argument("--output", type=Path, default=Path("stage8c-fresh-seed-report.json"))
    args = ap.parse_args()

    dev_raw, dev = load(args.development)
    fresh_raw, fresh = load(args.fresh)
    if {r["corpus_seed"] for r in dev} != DEV_SEEDS:
        raise ValueError("development corpus does not contain exactly the frozen Stage 8C families")
    if {r["corpus_seed"] for r in fresh} != FRESH_SEEDS:
        raise ValueError("fresh corpus does not contain exactly the four predeclared Stage 8C families")
    if DEV_SEEDS & FRESH_SEEDS:
        raise ValueError("development/fresh seed leakage")
    require_target_v2(dev)
    require_target_v2(fresh)

    models = {}
    records = {}
    feature_sets = {
        "public_semantics": lambda r, c: public_features(r, c, "public_semantics"),
        "target_semantics": target_features,
    }
    for name, feature_fn in feature_sets.items():
        weights = train_weights(dev, feature_fn)
        model_records = evaluate(fresh, weights, feature_fn)
        records[name] = model_records
        models[name] = {
            "overall": base.aggregate(model_records),
            "per_seed": game_seed_macro(model_records, fresh),
        }

    diffs = []
    for seed in sorted(FRESH_SEEDS):
        old = models["public_semantics"]["per_seed"][str(seed)]["game_macro"]
        new = models["target_semantics"]["per_seed"][str(seed)]["game_macro"]
        diffs.append({
            "seed": seed,
            "regret_improvement": old["normalized_regret"] - new["normalized_regret"],
            "top1_delta": new["top1_accuracy"] - old["top1_accuracy"],
        })

    mean_regret_improvement = sum(x["regret_improvement"] for x in diffs) / len(diffs)
    mean_top1_delta = sum(x["top1_delta"] for x in diffs) / len(diffs)
    families_better = sum(x["regret_improvement"] > 0 for x in diffs)
    positive = (
        mean_regret_improvement >= PRIMARY_MIN_REGRET_IMPROVEMENT
        and families_better >= PRIMARY_MIN_FAMILIES_BETTER
        and mean_top1_delta >= PRIMARY_MIN_TOP1_DELTA
    )

    report = {
        "schema_version": "stage8c-fresh-seed-v1",
        "target_semantics_version": TARGET_SEMANTICS_VERSION,
        "promotion_allowed": False,
        "development_sha256": hashlib.sha256(dev_raw).hexdigest(),
        "fresh_sha256": hashlib.sha256(fresh_raw).hexdigest(),
        "development_seeds": sorted(DEV_SEEDS),
        "fresh_seeds": sorted(FRESH_SEEDS),
        "config": base.CONFIG,
        "models": models,
        "primary_contrast": {
            "target_semantics_vs_public_semantics": diffs,
            "seed_macro_regret_improvement": mean_regret_improvement,
            "families_with_lower_regret": families_better,
            "seed_macro_top1_delta": mean_top1_delta,
            "thresholds": {
                "min_regret_improvement": PRIMARY_MIN_REGRET_IMPROVEMENT,
                "min_families_better": PRIMARY_MIN_FAMILIES_BETTER,
                "min_top1_delta": PRIMARY_MIN_TOP1_DELTA,
            },
            "positive_replication": positive,
        },
        "limitations": [
            "Offline imitation of Forge fixed-root labels only.",
            "No learned model controls gameplay.",
            "Passing and uncaptured priority windows remain outside proposal coverage.",
            "A positive replication would justify shadow execution instrumentation, not live policy control.",
        ],
    }
    args.output.write_text(json.dumps(report, sort_keys=True, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report["primary_contrast"], sort_keys=True))
    print("Offline only; promotion_allowed=false")


if __name__ == "__main__":
    main()
