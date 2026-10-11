#!/usr/bin/env python3
"""Test-only isolated actor. Proposes basic lands, two creatures, Flame Rift.

Player-visible input is strictly objective + own cards/decklist + visible
opponent battlefield + referee-owned dual life totals. Referee is the ONLY
rules authority; never accept hidden game internals or external card catalog.
"""
import json
import sys

BASICS = frozenset(("Plains", "Island", "Swamp", "Mountain", "Forest", "Wastes"))
CREATURES = frozenset(("Llanowar Elves", "Grizzly Bears"))
FIELDS = frozenset(("objective", "own_cards", "opponent_table", "life_totals"))

for raw in sys.stdin.buffer:
    if len(raw) > 16384:
        raise SystemExit("oversized view")
    view = json.loads(raw)
    if not isinstance(view, dict) or set(view) != FIELDS:
        raise SystemExit("unapproved view")
    own = view["own_cards"]
    if not isinstance(own, dict) or set(own) != {
            "hand", "decklist_multiset", "table"}:
        raise SystemExit("unapproved own cards")
    hand = own["hand"]
    if not isinstance(hand, list) or any(
            not isinstance(name, str) or not name for name in hand):
        raise SystemExit("invalid own hand")
    totals = view["life_totals"]
    if not isinstance(totals, dict) or set(totals) != {
            "self", "opponent"} or any(type(n) is not int for n in totals.values()):
        raise SystemExit("invalid referee life totals")
    opponent_cards = view["opponent_table"]
    if not isinstance(opponent_cards, list):
        raise SystemExit("invalid visible opponent table")
    for card in opponent_cards:
        if not isinstance(card, dict):
            raise SystemExit("invalid opponent card")
        if card.get("appearance") == "face_down":
            if set(card) != {"appearance"}:
                raise SystemExit("concealed original leaked")
        elif card.get("appearance") == "face_up":
            if set(card) != {"appearance", "card"} or not isinstance(
                    card["card"], str):
                raise SystemExit("invalid visible card")
        else:
            raise SystemExit("invalid face state")

    intent = {"intent": "pass"}
    for index, name in enumerate(hand):
        if name in BASICS:
            intent = {"intent": "play_land", "hand_index": index}
            break
    else:
        for index, name in enumerate(hand):
            if name in CREATURES:
                intent = {"intent": "cast_creature", "hand_index": index}
                break
        else:
            for index, name in enumerate(hand):
                if name == "Flame Rift":
                    intent = {"intent": "cast_sorcery", "hand_index": index}
                    break
    print(json.dumps(intent, sort_keys=True), flush=True)
