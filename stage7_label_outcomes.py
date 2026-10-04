#!/usr/bin/env python3
"""Attach terminal Forge game outcomes to Stage-7 decision rows.

The label is from the acting player's perspective: 1.0 win, 0.0 loss, 0.5 draw.
Rows are associated with a game only by their position in Forge's sequential log;
no hidden zone is inspected and no gameplay/card semantics are changed.

`game_group` is a stable corpus-level identifier used for train/validation splitting.
It deliberately includes the source log name because game_index restarts in every log.
"""
from __future__ import annotations
import argparse, json, re
from pathlib import Path

DATA = "EXPERT_STAGE7_DATA: "
RESULT = "Game Result: Game "
WINNER = re.compile(r"Ai\((\d+)\)-.* has won!")


def label_log(path: Path) -> list[dict]:
    out: list[dict] = []
    pending: list[dict] = []
    game_index = 0
    source_group = path.stem
    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if raw.startswith(DATA):
            pending.append(json.loads(raw[len(DATA):]))
            continue
        if not raw.startswith(RESULT):
            continue
        game_index += 1
        if "ended in a Draw!" in raw:
            winner = None
        else:
            m = WINNER.search(raw)
            if not m:
                raise ValueError(f"unrecognized Forge result line: {raw}")
            winner = int(m.group(1))
        for row in pending:
            actor = int(row["acting_player"])
            row["game_index"] = game_index
            row["game_group"] = f"{source_group}:game-{game_index}"
            row["game_result"] = 0.5 if winner is None else (1.0 if actor == winner else 0.0)
            out.append(row)
        pending.clear()
    if pending:
        raise ValueError(f"{len(pending)} decision rows were not followed by a terminal Forge result")
    if not out:
        raise ValueError("no labeled Stage-7 decision rows found")
    return out


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("log", type=Path)
    ap.add_argument("output", type=Path)
    a = ap.parse_args()
    rows = label_log(a.log)
    a.output.write_text("".join(json.dumps(r, sort_keys=True, separators=(",", ":")) + "\n" for r in rows), encoding="utf-8")
    games = len({r["game_group"] for r in rows})
    print(f"Labeled {len(rows)} legal decisions across {games} completed Forge games.")

if __name__ == "__main__":
    main()
