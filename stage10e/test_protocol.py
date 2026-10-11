#!/usr/bin/env python3
"""Executable Stage10E menace actor privacy canary; not a full Forge or sandbox proof."""
import json
import subprocess
import sys
from pathlib import Path

WORKER = Path(__file__).with_name("menace_defense_actor.py")
EXACT_REPLY = {"intent", "land_hand_indices", "creature_hand_indices",
               "attack_table_indices", "block_pairs"}
EXACT_VIEW = {"objective", "own_cards", "opponent_table", "life_totals"}


def observation():
    return {
        "objective": "Win the game under referee-enforced Magic rules",
        "own_cards": {
            "hand": ["Forest", "Grizzly Bears"],
            "decklist_multiset": [["Forest", 24], ["Grizzly Bears", 4],
                                  ["Llanowar Elves", 4]],
            "table": [{"appearance": "face_up", "card": "Grizzly Bears"},
                      {"appearance": "face_down"},
                      {"appearance": "face_up", "card": "Grizzly Bears"}],
        },
        "opponent_table": [
            {"appearance": "face_up", "card": "Grizzly Bears"},
            {"appearance": "face_down"},
        ],
        "life_totals": {"self": 20, "opponent": 20},
    }


def launch():
    return subprocess.Popen(
        [sys.executable, "-I", "-S", "-u", str(WORKER.resolve())],
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        text=True,
    )


def ask(process, request):
    process.stdin.write(json.dumps(request, separators=(",", ":")) + "\n")
    process.stdin.flush()
    line = process.stdout.readline()
    if not line:
        raise AssertionError("worker exited instead of responding")
    return json.loads(line)


def valid_plan(result):
    assert set(result) == EXACT_REPLY, result
    assert result["intent"] == "action_plan"
    assert result["block_pairs"] == [[0, 0], [2, 0]], result
    assert len({p[0] for p in result["block_pairs"]}) == 2
    assert len({p[1] for p in result["block_pairs"]}) == 1
    assert result["attack_table_indices"] == [0, 2]
    assert all(isinstance(i, int) and not isinstance(i, bool)
               for pair in result["block_pairs"] for i in pair)


def run():
    assert WORKER.is_file(), "missing checked-in Stage10E worker"
    a, b = launch(), launch()
    try:
        assert a.pid != b.pid, "actors must run as different OS processes"
        for process in (a, b):
            for _ in range(2):
                valid_plan(ask(process, observation()))
        opening = ask(a, {"request": "own_opening_hand",
                          "observation": observation()})
        assert set(opening) == {"intent", "keep", "bottom_priority"}
        assert opening["intent"] == "opening_hand"
        assert sorted(opening["bottom_priority"]) == [0, 1]
        alternate = observation()
        alternate["life_totals"] = {"self": 3, "opponent": 99}
        assert ask(b, alternate)["block_pairs"] == [[0, 0], [2, 0]]
        assert set(observation()) == EXACT_VIEW
    finally:
        for p in (a, b):
            p.stdin.close()
            p.terminate()
            try:
                p.wait(timeout=3)
            except subprocess.TimeoutExpired:
                p.kill()
                p.wait(timeout=3)
            p.stdout.close()
            p.stderr.close()

    for illegal_key in ("phase", "turn", "priority", "stack",
                        "opponent_hand", "library_order", "referee_state"):
        illegal = observation()
        illegal[illegal_key] = "forbidden"
        proc = subprocess.run(
            [sys.executable, "-I", "-S", "-u", str(WORKER.resolve())],
            input=json.dumps(illegal) + "\n", text=True,
            capture_output=True, timeout=5,
        )
        assert proc.returncode != 0, illegal_key
        assert "observation contract violation" in proc.stderr, illegal_key
        assert not proc.stdout.strip(), illegal_key
    # Verify the other preference branch with two separately visible targets.
    visible_two=observation()
    visible_two["opponent_table"].insert(1, {"appearance":"face_up","card":"Grizzly Bears"})
    pair_result=subprocess.run(
        [sys.executable,"-I","-S","-u",str(WORKER.resolve())],
        input=json.dumps(visible_two)+"\\n",text=True,capture_output=True,timeout=5
    )
    assert pair_result.returncode==0, pair_result.stderr
    assert json.loads(pair_result.stdout)["block_pairs"]==[[0,0],[2,1]]

    # All these messages are malformed projections; the actor must fail closed.
    malformed=[]
    x=observation();x["own_cards"]["table"][1]["card"]="Concealed True Name";malformed.append(x)
    x=observation();x["opponent_table"][-1]["card"]="Hidden Opponent Card";malformed.append(x)
    x=observation();x["opponent_table"][0]["original_card"]="Secret";malformed.append(x)
    x=observation();x["life_totals"]["self"]=True;malformed.append(x)
    x=observation();x["life_totals"]["opponent"]="20";malformed.append(x)
    x=observation();x["own_cards"]["decklist_multiset"][0][1]="24";malformed.append(x)
    x=observation();x["own_cards"]["hand"]=[{"card":"Forest"}];malformed.append(x)
    for i,invalid in enumerate(malformed):
        proc=subprocess.run([sys.executable,"-I","-S","-u",str(WORKER.resolve())],
            input=json.dumps(invalid)+"\\n",text=True,capture_output=True,timeout=5)
        assert proc.returncode!=0, (i,proc.stdout)
        assert not proc.stdout.strip(), (i,proc.stdout)
    print("Stage10E menace actor IPC PASS: two independent persistent workers,"
          " exactly two candidates for one visible target, separate-target fallback,"
          " face-down secrecy, life and deck type checks, forbidden fields")


if __name__ == "__main__":
    run()
