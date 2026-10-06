#!/usr/bin/env python3
"""Canonical Stage 8A JSONL serializer.

Consumes one JSON object per captured Forge decision and emits learner-boundary
stage8a-v1 JSONL. This module does not infer legality or card rules: Forge owns
those semantics. It only normalizes already aggregated fixed-root observations.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

FORGE_VERSION = "2.0.15"
SEARCH_POLICY = "fixed-root-v1"
SCHEMA = "stage8a-v1"


def normalize(raw: dict) -> dict:
    required = {
        "decision_id", "corpus_seed", "game_group", "public_state",
        "selected_action", "candidates", "information_set_samples",
    }
    missing = required - set(raw)
    if missing:
        raise ValueError(f"missing capture fields: {sorted(missing)}")
    samples = raw["information_set_samples"]
    if not isinstance(samples, int) or isinstance(samples, bool) or samples < 2:
        raise ValueError("information_set_samples must be integer >=2")
    if not isinstance(raw["public_state"], dict):
        raise ValueError("public_state must be an object")
    if not isinstance(raw["candidates"], list):
        raise ValueError("candidates must be a list")

    candidates = []
    for c in raw["candidates"]:
        if not isinstance(c, dict):
            raise ValueError("candidate must be an object")
        identity = c.get("action_identity")
        score = c.get("aggregate_score")
        replay = c.get("replay_valid_count")
        if not isinstance(identity, str) or not identity:
            raise ValueError("candidate action_identity must be non-empty string")
        if not isinstance(score, int) or isinstance(score, bool):
            raise ValueError("candidate aggregate_score must be integer")
        if replay != samples:
            raise ValueError("partial-world candidate cannot be serialized")
        normalized = {
            "action_identity": identity,
            "aggregate_score": score,
            "replay_valid_count": replay,
        }
        if "target_public_semantics" in c:
            target_semantics = c["target_public_semantics"]
            if (not isinstance(target_semantics, list)
                    or not all(isinstance(x, str) and x for x in target_semantics)):
                raise ValueError("target_public_semantics must be a list of non-empty public descriptors")
            if any("id=" in x.lower() for x in target_semantics):
                raise ValueError("raw object IDs may not cross the learner boundary")
            normalized["target_public_semantics"] = target_semantics
        candidates.append(normalized)

    # Preserve Forge's deterministic first-seen candidate order. Sorting by score
    # could make serialization depend on hidden-world-derived labels.
    return {
        "schema_version": SCHEMA,
        "decision_id": raw["decision_id"],
        "corpus_seed": raw["corpus_seed"],
        "game_group": raw["game_group"],
        "public_state": raw["public_state"],
        "selected_action": raw["selected_action"],
        "candidates": candidates,
        "information_set_samples": samples,
        "forge_version": FORGE_VERSION,
        "search_policy": SEARCH_POLICY,
    }


def canonical_line(row: dict) -> str:
    return json.dumps(row, sort_keys=True, separators=(",", ":"), ensure_ascii=False)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("capture", type=Path)
    ap.add_argument("output", type=Path)
    args = ap.parse_args()
    lines = []
    for line_no, text in enumerate(args.capture.read_text(encoding="utf-8").splitlines(), 1):
        if not text.strip():
            continue
        try:
            lines.append(canonical_line(normalize(json.loads(text))))
        except Exception as exc:
            raise ValueError(f"capture line {line_no}: {exc}") from exc
    if not lines:
        raise ValueError("empty Stage 8 capture")
    args.output.write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
