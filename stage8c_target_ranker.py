#!/usr/bin/env python3
"""Offline Stage 8C comparison: Stage 8B public semantics vs target-aware semantics."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

import stage8_action_ranker as base
from stage8_ranker_features import features as stage8b_features
from stage8c_target_features import features as target_features


MODELS = ("public_semantics", "target_semantics")


def compare(rows):
    base.validate_rows(rows)
    seeds = sorted({r["corpus_seed"] for r in rows})
    if len(seeds) < 3:
        raise ValueError("Stage 8C requires at least three independent seed families")

    vectors = {
        "public_semantics": {
            r["decision_id"]: [stage8b_features(r, c, "public_semantics") for c in r["candidates"]]
            for r in rows
        },
        "target_semantics": {
            r["decision_id"]: [target_features(r, c) for c in r["candidates"]]
            for r in rows
        },
    }

    report = {}
    for model in MODELS:
        folds = []
        records_all = []
        for seed in seeds:
            train_rows = [r for r in rows if r["corpus_seed"] != seed]
            holdout = [r for r in rows if r["corpus_seed"] == seed]
            weights = base.train(
                train_rows,
                vectors[model],
                epochs=base.CONFIG["epochs"],
                lr=base.CONFIG["learning_rate"],
            )
            records = []
            for row in holdout:
                predictions = [base.dot(weights, x) for x in vectors[model][row["decision_id"]]]
                records.append(base.decision_metrics(row, predictions))
            records_all.extend(records)
            folds.append({
                "holdout_seed": seed,
                "train_seeds": [s for s in seeds if s != seed],
                "holdout": base.aggregate(records),
            })
        report[model] = {"folds": folds, "holdout": base.aggregate(records_all)}

    old = report["public_semantics"]["holdout"]
    new = report["target_semantics"]["holdout"]
    return {
        "schema_version": "stage8c-target-ranking-v1",
        "method": "leave-one-seed-out-public-target-semantics",
        "promotion_allowed": False,
        "seed_count": len(seeds),
        "config": base.CONFIG,
        "models": report,
        "target_minus_public_semantics": {
            "top1_delta": new["top1_accuracy"] - old["top1_accuracy"],
            "pairwise_delta": (
                None if old["pairwise_accuracy"] is None or new["pairwise_accuracy"] is None
                else new["pairwise_accuracy"] - old["pairwise_accuracy"]
            ),
            "normalized_regret_delta": new["normalized_regret"] - old["normalized_regret"],
        },
        "limitations": [
            "Development comparison only; these seed families are not an untouched promotion test.",
            "Forge fixed-root scores remain imitation labels, not independent proof of optimal play.",
            "Target semantics contain public descriptors only; raw Forge object IDs are discarded before serialization.",
            "No learned model controls gameplay.",
        ],
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("jsonl", type=Path)
    ap.add_argument("--output", type=Path, default=Path("stage8c-target-ranking.json"))
    args = ap.parse_args()
    raw = args.jsonl.read_bytes()
    rows = [json.loads(x) for x in raw.splitlines() if x.strip()]
    result = compare(rows)
    result["dataset_sha256"] = hashlib.sha256(raw).hexdigest()
    args.output.write_text(json.dumps(result, sort_keys=True, indent=2) + "\n", encoding="utf-8")
    for name, model in result["models"].items():
        h = model["holdout"]
        print(f"{name}: top1={h['top1_accuracy']:.4f} normalized_regret={h['normalized_regret']:.4f} pairwise={h['pairwise_accuracy']}")
    print("Offline only; promotion_allowed=false")


if __name__ == "__main__":
    main()
