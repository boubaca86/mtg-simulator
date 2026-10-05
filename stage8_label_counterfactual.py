#!/usr/bin/env python3
"""Attach whole-game grouping to live Stage 8A counterfactual captures.

Forge remains the rules referee and score producer. This parser never inspects
hidden zones; it only groups already-legal capture events by terminal game
boundaries and emits raw rows for stage8_serialize_counterfactual.py.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

CAPTURE = "EXPERT_STAGE8_CAPTURE: "
RESULT = "Game Result: Game "
GAME_NUMBER = re.compile(r"^Game Result: Game (\d+) ")
AUDIT = re.compile(r"^EXPERT_INFOSET_AUDITED_CANDIDATE: candidate=\d+ identifiedWorlds=(\d+) samples=(\d+)$")


def parse_log(path: Path, expected_games: int | None = None, samples: int = 3,
              corpus_seed: int | None = None) -> list[dict]:
    out: list[dict] = []
    pending: list[dict] = []
    source_group = path.stem
    game_index = 0
    audits = 0

    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if "Stopping slow match as draw" in raw:
            raise ValueError("timeout: source log is quarantined")
        if raw.startswith("EXPERT_INFOSET_STRATEGY_FUSION:"):
            raise ValueError("strategy-fusion divergence: source log is quarantined")
        if raw.startswith("EXPERT_INFOSET_ACTION_REPLAY_MISMATCH:"):
            raise ValueError("action replay mismatch: source log is quarantined")
        if re.search(r"(?:Exception in thread|java\.[\w.]+(?:Exception|Error)|OutOfMemoryError|StackOverflowError)", raw):
            raise ValueError("Forge exception: source log is quarantined")

        audit = AUDIT.fullmatch(raw)
        if audit:
            if tuple(map(int, audit.groups())) != (samples, samples):
                raise ValueError("incomplete hidden-world action audit")
            audits += 1
            continue

        if raw.startswith(CAPTURE):
            row = json.loads(raw[len(CAPTURE):])
            if row.get("schema_version") != "stage8-capture-v1":
                raise ValueError("wrong live capture schema")
            if row.get("information_set_samples") != samples:
                raise ValueError("wrong information-set sample count")
            if not isinstance(row.get("public_state"), dict):
                raise ValueError("capture lacks legal public_state object")
            if not row.get("selected_action"):
                raise ValueError("capture lacks selected action")
            candidates = row.get("candidates")
            if not isinstance(candidates, list):
                raise ValueError("capture candidates must be a list")
            pending.append(row)
            continue

        if not raw.startswith(RESULT):
            continue

        game_index += 1
        number = GAME_NUMBER.match(raw)
        if not number or int(number.group(1)) != game_index:
            raise ValueError("missing, repeated or out-of-order terminal game number")
        if not pending or not audits:
            raise ValueError("each completed game needs Stage 8 rows and complete-world audit coverage")

        for row in pending:
            decision_index = row.pop("decision_index")
            row.pop("schema_version", None)
            row.pop("matchup_id", None)
            row["decision_id"] = f"{source_group}:{decision_index}"
            row["game_group"] = f"{source_group}:game-{game_index}"
            if corpus_seed is not None:
                row["corpus_seed"] = corpus_seed
            elif "corpus_seed" not in row:
                row["corpus_seed"] = row.get("run_seed")
            row.pop("run_seed", None)
            out.append(row)
        pending.clear()
        audits = 0

    if pending:
        raise ValueError(f"{len(pending)} Stage 8 rows were not followed by a terminal game result")
    if not out:
        raise ValueError("no live Stage 8 captures found")
    if expected_games is not None and game_index != expected_games:
        raise ValueError(f"expected {expected_games} completed games, found {game_index}")
    return out


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("log", type=Path)
    ap.add_argument("output", type=Path)
    ap.add_argument("--expected-games", type=int)
    ap.add_argument("--samples", type=int, default=3)
    ap.add_argument("--corpus-seed", type=int)
    a = ap.parse_args()
    rows = parse_log(a.log, a.expected_games, a.samples, a.corpus_seed)
    a.output.write_text("".join(json.dumps(r, sort_keys=True, separators=(",", ":")) + "\n" for r in rows), encoding="utf-8")
    print(f"Grouped {len(rows)} Stage 8 counterfactual decisions across {len({r['game_group'] for r in rows})} games.")


if __name__ == "__main__":
    main()
