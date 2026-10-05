#!/usr/bin/env python3
"""Attach terminal Forge game outcomes to Stage-7 decision rows.

The label is from the acting player's perspective: 1.0 win, 0.0 loss, 0.5 draw.
Rows are associated with a game only by their position in Forge's sequential log;
no hidden zone is inspected and no gameplay/card semantics are changed.

Forge 2.0.15's CLI names seats Ai(1)/Ai(2), while Game assigns IDs 0/1.
The explicit conversion below is part of the data contract, not a heuristic.
Timeouts, incomplete audits and strategy-fusion diagnostics invalidate the log.
"""
from __future__ import annotations
import argparse, json, re
from pathlib import Path

DATA = "EXPERT_STAGE7_DATA: "
RESULT = "Game Result: Game "
WINNER = re.compile(r"Ai\((\d+)\)-.* has won!")
GAME_NUMBER = re.compile(r"^Game Result: Game (\d+) ")
AUDIT = re.compile(r"^EXPERT_INFOSET_AUDITED_CANDIDATE: candidate=\d+ identifiedWorlds=(\d+) samples=(\d+)$")
LABEL_VERSION = "forge-cli-zero-based-v2"


def label_log(path: Path, expected_games: int | None = None, samples: int = 3) -> list[dict]:
    out: list[dict] = []
    pending: list[dict] = []
    game_index = 0
    audits = 0
    source_group = path.stem
    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if "Stopping slow match as draw" in raw:
            raise ValueError("timeout: the entire source log is quarantined, even if a later line reports a winner")
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
        if raw.startswith(DATA):
            row = json.loads(raw[len(DATA):])
            actor = row.get("acting_player")
            if type(actor) is not int or actor not in (0, 1):
                raise ValueError("acting_player must be a zero-based Forge player ID (0 or 1)")
            if row.get("infoset_sample_count") != samples:
                raise ValueError("decision row has the wrong information-set sample count")
            if not row.get("complete_action_identity"):
                raise ValueError("decision row lacks complete_action_identity")
            name = row.get("acting_player_name")
            if name is not None and not name.startswith(f"Ai({actor + 1})-"):
                raise ValueError("acting_player_name disagrees with the Forge player ID")
            pending.append(row)
            continue
        if not raw.startswith(RESULT):
            continue
        game_index += 1
        number = GAME_NUMBER.match(raw)
        if not number or int(number.group(1)) != game_index:
            raise ValueError("missing, repeated or out-of-order terminal game number")
        if not pending or not audits:
            raise ValueError("each completed game must have decision rows and complete-world audit coverage")
        if "ended in a Draw!" in raw:
            winner = None
        else:
            m = WINNER.search(raw)
            if not m:
                raise ValueError(f"unrecognized Forge result line: {raw}")
            # Display seat numbers are one-based; exported Player.getId() is zero-based.
            winner = int(m.group(1)) - 1
            if winner not in (0, 1):
                raise ValueError("terminal winner is outside the two-player Forge CLI contract")
        for row in pending:
            actor = int(row["acting_player"])
            row["game_index"] = game_index
            row["game_group"] = f"{source_group}:game-{game_index}"
            row["game_result"] = 0.5 if winner is None else (1.0 if actor == winner else 0.0)
            row["label_version"] = LABEL_VERSION
            row["terminal_winner_player"] = winner
            out.append(row)
        pending.clear()
        audits = 0
    if pending:
        raise ValueError(f"{len(pending)} decision rows were not followed by a terminal Forge result")
    if not out:
        raise ValueError("no labeled Stage-7 decision rows found")
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
    rows = label_log(a.log, a.expected_games, a.samples)
    if a.corpus_seed is not None:
        for row in rows:
            row['corpus_seed'] = a.corpus_seed
    a.output.write_text("".join(json.dumps(r, sort_keys=True, separators=(",", ":")) + "\n" for r in rows), encoding="utf-8")
    games = len({r["game_group"] for r in rows})
    print(f"Labeled {len(rows)} legal decisions across {games} completed Forge games.")

if __name__ == "__main__":
    main()
