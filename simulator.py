import csv
import json
import random
from datetime import datetime, timezone
from pathlib import Path

GAMES_PER_RUN = 100_000

RESULTS_DIR = Path("results")
STATE_FILE = RESULTS_DIR / "summary.json"
HISTORY_FILE = RESULTS_DIR / "history.csv"

RESULTS_DIR.mkdir(exist_ok=True)

# Load previous results if they exist
if STATE_FILE.exists():
    with open(STATE_FILE, "r") as f:
        state = json.load(f)
else:
    state = {
        "runs": 0,
        "total_games": 0,
        "player_a_wins": 0,
        "player_b_wins": 0
    }

# Play this batch
batch_a_wins = 0
batch_b_wins = 0

for _ in range(GAMES_PER_RUN):
    if random.random() < 0.5:
        batch_a_wins += 1
    else:
        batch_b_wins += 1

# Add this batch to all previous results
state["runs"] += 1
state["total_games"] += GAMES_PER_RUN
state["player_a_wins"] += batch_a_wins
state["player_b_wins"] += batch_b_wins
state["last_run_utc"] = datetime.now(timezone.utc).isoformat()

total = state["total_games"]

state["player_a_win_rate"] = state["player_a_wins"] / total * 100
state["player_b_win_rate"] = state["player_b_wins"] / total * 100

# Save permanent state
with open(STATE_FILE, "w") as f:
    json.dump(state, f, indent=2)

# Save history of every run
new_history = not HISTORY_FILE.exists()

with open(HISTORY_FILE, "a", newline="") as f:
    writer = csv.writer(f)

    if new_history:
        writer.writerow([
            "run",
            "time_utc",
            "games_this_run",
            "player_a_wins_this_run",
            "player_b_wins_this_run",
            "total_games",
            "player_a_total_wins",
            "player_b_total_wins"
        ])

    writer.writerow([
        state["runs"],
        state["last_run_utc"],
        GAMES_PER_RUN,
        batch_a_wins,
        batch_b_wins,
        state["total_games"],
        state["player_a_wins"],
        state["player_b_wins"]
    ])

print("=" * 60)
print("MTG CLOUD SIMULATOR")
print("=" * 60)
print(f"Run number: {state['runs']}")
print(f"Games this run: {GAMES_PER_RUN:,}")
print()
print(f"TOTAL GAMES: {state['total_games']:,}")
print(f"Player A total wins: {state['player_a_wins']:,}")
print(f"Player B total wins: {state['player_b_wins']:,}")
print()
print(f"Player A cumulative win rate: {state['player_a_win_rate']:.2f}%")
print(f"Player B cumulative win rate: {state['player_b_win_rate']:.2f}%")
print("=" * 60)
