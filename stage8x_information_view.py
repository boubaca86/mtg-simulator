"""Minimal player-observable MTG information view.

Does not run Forge rules or enumerate cards from Magic's entire catalog.
Accept only a trusted public snapshot, the player's own deck multiset, and
cards the player legitimately saw during the current game.
"""
from __future__ import annotations
from collections import Counter
import hashlib
import json

SCHEMA = "stage8x-observed-only-v1"
ALLOWED_PUBLIC = frozenset({
    "turn", "phase", "acting_life", "opponent_life",
    "own_hand", "own_battlefield", "opponent_battlefield",
    "own_graveyard", "opponent_graveyard",
    "exile_public", "stack_public",
    "opponent_unknown_hand_count", "own_library_count",
    "opponent_library_count",
})

def information_view(*, own_decklist: list[str], public: dict,
                     observed_opponent_cards: list[str]) -> dict:
    if not isinstance(public, dict) or not set(public) <= ALLOWED_PUBLIC:
        raise ValueError("Secret or unrecognized public state")
    for key in ("own_hand", "opponent_unknown_hand_count", "own_library_count",
                "opponent_library_count"):
        if key not in public:
            raise ValueError(f"Missing observable field: {key}")
    for value in (own_decklist, observed_opponent_cards):
        if not isinstance(value, list) or any(not isinstance(c,str) or not c for c in value):
            raise ValueError("Expected observed card names")
    if not own_decklist:
        raise ValueError("Player decklist required")
    if not isinstance(public["own_hand"],list) or any(not isinstance(c,str) for c in public["own_hand"]):
        raise ValueError("Invalid own hand")
    for key in ("opponent_unknown_hand_count", "own_library_count", "opponent_library_count"):
        if type(public[key]) is not int or public[key] < 0:
            raise ValueError("Invalid hidden-zone count")
    return {
        "schema_version": SCHEMA,
        "own_decklist_counts": dict(sorted(Counter(own_decklist).items())),
        # Historical sightings aren't assertions about current hidden locations.
        "observed_opponent_card_names": sorted(set(observed_opponent_cards)),
        "opponent_unknown_hand_slots": public["opponent_unknown_hand_count"],
        "opponent_unknown_library_slots": public["opponent_library_count"],
        "own_unknown_library_order": True,
        "public": {k: public[k] for k in sorted(public)},
    }

def fingerprint(view: dict) -> str:
    if not isinstance(view,dict) or view.get("schema_version") != SCHEMA:
        raise ValueError("Invalid information view")
    return hashlib.sha256(
        json.dumps(view,sort_keys=True,separators=(",",":")).encode("utf-8")
    ).hexdigest()
