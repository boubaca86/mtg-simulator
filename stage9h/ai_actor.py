#!/usr/bin/env python3
"""Test-only AI process. Receives strictly projected messages, never Forge state."""
import hashlib
import json
import os
import sys

FIELDS = {"objective", "own_cards", "opponent_table", "life_totals"}
for raw in sys.stdin.buffer:
    if len(raw) > 16384:
        raise SystemExit("oversized observation")
    state = json.loads(raw)
    if not isinstance(state, dict) or set(state) != FIELDS:
        raise SystemExit("unapproved observation fields")
    if set(state["own_cards"]) != {"hand", "decklist_multiset", "table"}:
        raise SystemExit("unapproved own-card fields")
    if set(state["life_totals"]) != {"self", "opponent"}:
        raise SystemExit("unapproved life fields")
    if any(type(n) is not int for n in state["life_totals"].values()):
        raise SystemExit("noninteger life")
    for card in state["opponent_table"]:
        if not isinstance(card, dict) or card.get("appearance") not in ("face_down", "face_up"):
            raise SystemExit("bad visible card")
        if card["appearance"] == "face_down" and set(card) != {"appearance"}:
            raise SystemExit("concealed identity leak")
        if card["appearance"] == "face_up" and set(card) != {"appearance", "card"}:
            raise SystemExit("bad visible identity")
    print(json.dumps({
        "intent": "pass",
        "pid": os.getpid(),
        "observation_sha256": hashlib.sha256(raw.rstrip(b"\r\n")).hexdigest(),
        "self_life": state["life_totals"]["self"],
        "opponent_life": state["life_totals"]["opponent"],
    }, sort_keys=True), flush=True)
