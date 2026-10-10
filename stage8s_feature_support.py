#!/usr/bin/env python3
"""Stage 8S: descriptive feature support and family-paired uncertainty only.

Uses exactly the prior pinned Stage 8L/8N DEVELOPMENT games via Stage 8R's
fail-closed loader. Never trains on Stage 8P outcomes, sends gameplay,
changes legal moves, or promotes a controller. No tuning is performed.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import random
from collections import defaultdict
from pathlib import Path

import stage8o_outcome_policy as old
import stage8r_outcome_diagnostic as stage8r

SCHEMA = "stage8s-offline-support-uncertainty-v1"
RESAMPLES = 10000
BOOTSTRAP_SEED = 81502026
SELF_TARGET_FEATURES = (
    "q_gain_life_to_self_player",
    "q_pump_to_self_creature",
    "q_damage_to_self_creature",
    "q_fixed_damage_to_self_creature",
)


def feature_support(samples, feature_names):
    """Descriptive prevalence only; rows in one game are NOT independent."""
    expected = tuple(sorted(k for k in feature_names if k.startswith("q_")))
    if not expected or len(set(expected)) != len(expected):
        raise ValueError("missing or duplicate Stage 8Q features")
    allowed = set(old.TRAIN_SEEDS)
    if not samples or {r["family"] for r in samples} != allowed:
        raise ValueError("missing or unexpected development seed families")
    counts = {}
    for name in expected:
        active = [r for r in samples if abs(r["features"][name]) > 0.0]
        if any(not math.isfinite(r["features"][name]) for r in samples):
            raise ValueError("nonfinite feature")
        counts[name] = {
            "active_observations": len(active),
            "active_games": len({r["game"] for r in active}),
            "active_families": len({r["family"] for r in active}),
            "active_positive_outcome_games": len({
                r["game"] for r in active if r["outcome"] == 1.0
            }),
            "active_negative_outcome_games": len({
                r["game"] for r in active if r["outcome"] == 0.0
            }),
        }
    for name in SELF_TARGET_FEATURES:
        if name not in counts:
            raise ValueError("missing positive or negative self-target feature")
    return {
        "number_of_q_features": len(expected),
        "number_never_activated": sum(x["active_observations"] == 0 for x in counts.values()),
        "number_with_fewer_than_three_seed_families": sum(
            x["active_families"] < 3 for x in counts.values()
        ),
        "by_feature": counts,
        "beneficial_self_target_options_always_remain_legal": True,
        "interpretation": "observed-action support, not causal value or independent rows",
    }


def _paired_folds(r_folds, o_folds):
    rmap = {f["held_seed"]: f for f in r_folds}
    omap = {f["holdout_seed"]: f for f in o_folds}
    allowed = set(old.TRAIN_SEEDS)
    if (len(rmap) != len(r_folds) or len(omap) != len(o_folds)
            or set(rmap) != allowed or set(omap) != allowed):
        raise ValueError("paired folds must match exact development seed families")
    result = []
    for family in sorted(allowed):
        new, base = rmap[family], omap[family]
        if (new["held_games"] != 2 or base["held_games"] != 2
                or new["train_games"] != 22 or base["train_games"] != 22):
            raise ValueError("both paired deck orientations required")
        delta = {}
        for metric in ("logloss", "brier"):
            a, b = new["model"][metric], base["model"][metric]
            if not math.isfinite(a) or not math.isfinite(b) or a < 0 or b < 0:
                raise ValueError("invalid loss measurement")
            delta[metric] = a - b
        result.append({"family": family, "delta": delta})
    return result


def _percentile(values, p):
    ordered = sorted(values)
    return ordered[min(len(ordered)-1, max(0, math.ceil(p * len(ordered)) - 1))]


def family_bootstrap(paired, resamples=RESAMPLES, seed=BOOTSTRAP_SEED):
    """Percentile family bootstrap; resample entire two-orientation families."""
    if (len(paired) != len(old.TRAIN_SEEDS)
            or {p["family"] for p in paired} != set(old.TRAIN_SEEDS)
            or resamples < 1000):
        raise ValueError("bootstrap must use twelve unique development families")
    rng = random.Random(seed)
    k = len(paired)
    estimates = {metric: [] for metric in ("logloss", "brier")}
    for _ in range(resamples):
        selected = [paired[rng.randrange(k)] for _ in range(k)]
        for metric in estimates:
            estimates[metric].append(sum(p["delta"][metric] for p in selected) / k)
    return {
        metric: {
            "mean_paired_delta": sum(p["delta"][metric] for p in paired) / k,
            "percentile_95_low": _percentile(draws, .025),
            "percentile_95_high": _percentile(draws, .975),
            "direction": "lower is better for 8R minus frozen 8O",
            "interval_crosses_zero": min(draws) <= 0 <= max(draws)
                if False else _percentile(draws, .025) <= 0 <= _percentile(draws, .975),
        }
        for metric, draws in estimates.items()
    }


def export(early, later):
    records, corpus = stage8r.read_training(early, later)
    samples, names = stage8r.observations(records)
    support = feature_support(samples, names)
    new = stage8r.grouped_comparison(records, samples, names)
    older = old.cross_validation(records)
    pairs = _paired_folds(new["folds"], older["folds"])
    uncertainty = family_bootstrap(pairs)
    # Both algorithms report the same 24 equal-weighted held-out games.
    for metric in ("logloss", "brier"):
        observed = new["model"][metric] - new["stage8o_model"][metric]
        if abs(observed - uncertainty[metric]["mean_paired_delta"]) > 1e-10:
            raise ValueError("family-paired delta mismatches Stage 8R CV")
    source_names = (
        "stage8s_feature_support.py",
        "stage8r_outcome_diagnostic.py",
        "stage8q_target_effect_features.py",
        "stage8o_outcome_policy.py",
    )
    source = {
        name: hashlib.sha256((Path(__file__).resolve().parent / name).read_bytes()).hexdigest()
        for name in source_names
    }
    return {
        "schema": SCHEMA,
        "scope": "pinned-old-development-data-descriptive-only",
        "training_games": 24,
        "observed_action_rows": 331,
        "seed_families": list(old.TRAIN_SEEDS),
        "consumed_reserved_seed_families_not_used": list(stage8r.CONSUMED_RESERVED_SEEDS),
        "future_proposed_seeds_unverified_and_not_used": True,
        "source_sha256": source,
        "corpus_sha256": corpus,
        "feature_support": support,
        "paired_seed_family_differences": pairs,
        "family_bootstrap": {
            "resamples": RESAMPLES,
            "seed": BOOTSTRAP_SEED,
            "unit": "entire seed family containing both orientations",
            "interval_type": "descriptive percentile bootstrap; not a prospective performance gate",
            "metrics": uncertainty,
        },
        "stage8r_representation_improvement_gate_passed":
            new["stage8q_representation_improved_both_metrics"],
        "new_gameplay_executed": False,
        "new_training_or_model_selection": False,
        "forge_or_card_semantics_changed": False,
        "promotion_allowed": False,
        "strength_claim_allowed": False,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("stage8l", type=Path)
    parser.add_argument("stage8n", type=Path)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    report = export(args.stage8l, args.stage8n)
    args.report.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n")
    print(json.dumps({
        "never_activated_q_features": report["feature_support"]["number_never_activated"],
        "few_family_q_features": report["feature_support"]["number_with_fewer_than_three_seed_families"],
        "performance_claim": False,
        "new_gameplay": False,
    }, sort_keys=True))


if __name__ == "__main__":
    main()
