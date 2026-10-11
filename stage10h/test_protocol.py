#!/usr/bin/env python3
"""Stage10H actor IPC parser canary. Not a sandbox or full Forge rules proof."""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORKERS = (
    ROOT / "stage9w" / "opening_hand_actor.py",
    ROOT / "stage10b" / "action_plan_actor.py",
    ROOT / "stage10d" / "multi_block_actor.py",
    ROOT / "stage10e" / "menace_defense_actor.py",
)
VIEW_KEYS = {"objective", "own_cards", "opponent_table", "life_totals"}


def view():
    return {
        "objective": "Win only by referee-enforced Magic rules",
        "own_cards": {
            "hand": ["Forest", "Grizzly Bears"],
            "decklist_multiset": [["Forest", 24], ["Grizzly Bears", 4],
                                  ["Llanowar Elves", 4]],
            "table": [{"appearance": "face_up", "card": "Grizzly Bears"},
                      {"appearance": "face_down"},
                      {"appearance": "face_up", "card": "Grizzly Bears"}],
        },
        "opponent_table": [{"appearance": "face_up", "card": "Grizzly Bears"},
                           {"appearance": "face_down"}],
        "life_totals": {"self": 20, "opponent": 20},
    }


def launch(worker):
    return subprocess.Popen(
        [sys.executable, "-I", "-S", "-u", str(worker)],
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        text=True,
    )


def ask(actor, payload):
    actor.stdin.write(json.dumps(payload, separators=(",", ":")) + "\n")
    actor.stdin.flush()
    line = actor.stdout.readline()
    assert line, "persistent actor failed to respond to valid JSON"
    return json.loads(line)


def ensure_positive(worker):
    a, b = launch(worker), launch(worker)
    try:
        assert a.pid != b.pid, "actors must have separate OS process IDs"
        assert set(view()) == VIEW_KEYS
        for actor in (a, b):
            for _ in range(2):
                normal = ask(actor, view())
                assert isinstance(normal, dict)
                assert normal["intent"] in ("play_land", "action_plan", "pass", "attack", "cast_creature")
            opening = ask(actor, {"request": "own_opening_hand", "observation": view()})
            assert set(opening) == {"intent", "keep", "bottom_priority"}, opening
            assert opening["intent"] == "opening_hand"
            assert sorted(opening["bottom_priority"]) == [0, 1]
    finally:
        for actor in (a, b):
            if actor.poll() is None:
                actor.terminate()
            try:
                actor.wait(timeout=3)
            except subprocess.TimeoutExpired:
                actor.kill()
                actor.wait(timeout=3)
            actor.stdin.close()
            actor.stdout.close()
            actor.stderr.close()


def invalid_messages():
    base = json.dumps(view(), separators=(",", ":"))
    assert '"objective":' in base
    assert '"hand":' in base
    assert '"life_totals":{"self":20' in base
    assert '"card":"Grizzly Bears"' in base
    assert '"appearance":"face_down"' in base
    cases = [
        ("duplicate top objective", base.replace('"objective":', '"objective":"bait","objective":', 1), "duplicate JSON field"),
        ("duplicate own hand", base.replace('"hand":', '"hand":[],"hand":', 1), "duplicate JSON field"),
        ("duplicate life self", base.replace('"life_totals":{"self":20', '"life_totals":{"self":19,"self":20', 1), "duplicate JSON field"),
        ("duplicate card name", base.replace('"card":"Grizzly Bears"', '"card":"secret","card":"Grizzly Bears"', 1), "duplicate JSON field"),
        ("duplicate face-down appearance", base.replace('"appearance":"face_down"', '"appearance":"face_up","appearance":"face_down"', 1), "duplicate JSON field"),
        ("escaped duplicate top key", base.replace('"objective":', r'"\\u006fbjective":"bait","objective":', 1), "duplicate JSON field"),
        ("duplicate opening request", '{"request":"own_opening_hand","request":"own_opening_hand","observation":' + base + '}', "duplicate JSON field"),
        ("duplicate nested opening", '{"request":"own_opening_hand","observation":' + base.replace('"hand":', '"hand":[],"hand":', 1) + '}', "duplicate JSON field"),
        ("NaN literal", base.replace('"self":20', '"self":NaN', 1), "nonfinite JSON literal"),
        ("Infinity literal", base.replace('"opponent":20', '"opponent":Infinity', 1), "nonfinite JSON literal"),
        ("negative Infinity literal", base.replace('"self":20', '"self":-Infinity', 1), "nonfinite JSON literal"),
        ("float overflow", base.replace('"self":20', '"self":1e999', 1), "nonfinite JSON number"),
        ("forbidden phase", base.replace('"objective":', '"phase":"private","objective":', 1), "observation contract violation"),
        ("oversized wire message", base + (" " * 16384), "oversized observation"),
    ]
    return cases


def ensure_negative(worker, cases):
    for label, message, expected_error in cases:
        proc = subprocess.run(
            [sys.executable, "-I", "-S", "-u", str(worker)],
            input=message + "\n", text=True, capture_output=True, timeout=5,
        )
        assert proc.returncode != 0, (worker.name, label, proc.stdout)
        assert proc.stdout == "", (worker.name, label, proc.stdout)
        assert expected_error in proc.stderr, (worker.name, label, proc.stderr)


def run():
    cases = invalid_messages()
    assert len(cases) == 14
    for worker in WORKERS:
        assert worker.is_file(), worker
        ensure_positive(worker)
        ensure_negative(worker, cases)
    print("Stage10H actor IPC strict JSON PASS: four actor types, two persistent"
          " independent PIDs per positive test, 56 malformed negatives,"
          " valid opening/plan observations, unchanged information allowlist")


if __name__ == "__main__":
    run()
