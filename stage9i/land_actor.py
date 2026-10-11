#!/usr/bin/env python3
"""Stage9I minimal actor policy: play one known basic land, otherwise pass.

No Forge references, external card catalog, opponent secret info, turn data, or
rules-engine privileges. The referee decides whether the proposed play is legal.
"""
import json
import sys

BASICS = {"Plains", "Island", "Swamp", "Mountain", "Forest", "Wastes"}
REQUIRED = {"objective", "own_cards", "opponent_table", "life_totals"}
for raw in sys.stdin.buffer:
    if len(raw) > 16384:
        raise SystemExit("oversized referee message")
    view = json.loads(raw)
    if not isinstance(view, dict) or set(view) != REQUIRED:
        raise SystemExit("unapproved top-level keys")
    if set(view["own_cards"]) != {"hand", "decklist_multiset", "table"}:
        raise SystemExit("unapproved own-card data")
    if set(view["life_totals"]) != {"self", "opponent"}:
        raise SystemExit("unapproved life fields")
    for card in view["opponent_table"]:
        if card.get("appearance") == "face_down" and set(card) != {"appearance"}:
            raise SystemExit("concealed original was leaked")
        if card.get("appearance") == "face_up" and set(card) != {"appearance", "card"}:
            raise SystemExit("bad visible card")
    intent = {"intent": "pass"}
    for index, name in enumerate(view["own_cards"]["hand"]):
        if name in BASICS:
            intent = {"intent": "play_land", "hand_index": index}
            break
    print(json.dumps(intent, sort_keys=True), flush=True)
