"""Stage 7A legal-information feature contract.

This module is deliberately independent of Forge internals.  It defines the
serialized boundary that a later Forge extractor must satisfy before any
learned evaluator is trained.  Unknown hidden identities are represented only
by counts; revealed/known cards require an explicit public-knowledge marker.
"""
from __future__ import annotations

from dataclasses import dataclass, asdict
from typing import Iterable, Mapping, Sequence

SCHEMA_VERSION = "stage7a-v1"


@dataclass(frozen=True)
class KnownCard:
    name: str
    public_knowledge: bool


@dataclass(frozen=True)
class DecisionObservation:
    run_seed: int
    decision_index: int
    acting_player: int
    on_play: bool
    turn: int
    phase: str
    step: str
    acting_life: int
    opponent_life: int
    own_hand: tuple[str, ...]
    opponent_known_cards: tuple[KnownCard, ...]
    opponent_unknown_hand_count: int
    own_library_count: int
    opponent_library_count: int
    battlefield_public: tuple[str, ...]
    graveyard_public: tuple[str, ...]
    exile_public: tuple[str, ...]
    stack_public: tuple[str, ...]
    mana_public: Mapping[str, int]
    matchup_id: str
    complete_action_identity: str
    infoset_sample_count: int


def _require_nonnegative(name: str, value: int) -> None:
    if value < 0:
        raise ValueError(f"{name} must be non-negative")


def export_legal_features(obs: DecisionObservation) -> dict:
    """Return the only feature payload Stage 7 training is allowed to consume."""
    _require_nonnegative("opponent_unknown_hand_count", obs.opponent_unknown_hand_count)
    _require_nonnegative("own_library_count", obs.own_library_count)
    _require_nonnegative("opponent_library_count", obs.opponent_library_count)
    if obs.infoset_sample_count < 1:
        raise ValueError("infoset_sample_count must be positive")
    if not obs.complete_action_identity:
        raise ValueError("complete_action_identity is required")

    leaked = [c.name for c in obs.opponent_known_cards if not c.public_knowledge]
    if leaked:
        raise ValueError("opponent card identity is not public knowledge")

    return {
        "schema_version": SCHEMA_VERSION,
        "run_seed": obs.run_seed,
        "decision_index": obs.decision_index,
        "acting_player": obs.acting_player,
        "on_play": obs.on_play,
        "turn": obs.turn,
        "phase": obs.phase,
        "step": obs.step,
        "acting_life": obs.acting_life,
        "opponent_life": obs.opponent_life,
        "own_hand": list(obs.own_hand),
        "opponent_known_cards": [c.name for c in obs.opponent_known_cards],
        "opponent_unknown_hand_count": obs.opponent_unknown_hand_count,
        "own_library_count": obs.own_library_count,
        "opponent_library_count": obs.opponent_library_count,
        "battlefield_public": list(obs.battlefield_public),
        "graveyard_public": list(obs.graveyard_public),
        "exile_public": list(obs.exile_public),
        "stack_public": list(obs.stack_public),
        "mana_public": dict(obs.mana_public),
        "matchup_id": obs.matchup_id,
        "complete_action_identity": obs.complete_action_identity,
        "infoset_sample_count": obs.infoset_sample_count,
    }


def grouped_split_key(row: Mapping) -> int:
    """All decisions from one run/game seed must remain in the same data split."""
    return int(row["run_seed"])
