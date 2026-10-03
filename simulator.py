import random
from datetime import datetime

GAMES = 100000

player_a_wins = 0
player_b_wins = 0

for _ in range(GAMES):
    # Temporary test game.
    # We will replace this with the real MTG engine.
    if random.random() < 0.5:
        player_a_wins += 1
    else:
        player_b_wins += 1

print("=" * 50)
print("MTG CLOUD SIMULATOR")
print("=" * 50)
print(f"Time: {datetime.now()}")
print(f"Games played: {GAMES:,}")
print(f"Player A wins: {player_a_wins:,}")
print(f"Player B wins: {player_b_wins:,}")
print(f"Player A win rate: {player_a_wins / GAMES * 100:.2f}%")
print(f"Player B win rate: {player_b_wins / GAMES * 100:.2f}%")
print("=" * 50)
