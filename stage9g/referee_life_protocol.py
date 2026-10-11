"""Stage 9G: minimal AI observations with referee-owned shared life totals.

TEST-ONLY boundary, not a Forge adapter, rules engine, or hardened OS sandbox.
No actor is passed this referee object; isolated workers get observation bytes only.
"""
from __future__ import annotations

from collections import Counter
from dataclasses import dataclass, replace
import json
from types import MappingProxyType
from typing import Mapping


@dataclass(frozen=True, slots=True)
class TableCard:
    controller: str
    owner: str
    private_name: str
    visible_name: str | None
    face_down: bool = False
    played_on_table: bool = True
    actor_may_look_original: frozenset[str] = frozenset()


@dataclass(frozen=True, slots=True)
class RefereeMemory:
    """Private information: remains in referee process, never in actor IPC."""
    objective: str
    hands: Mapping[str, tuple[str, ...]]
    libraries_in_draw_order: Mapping[str, tuple[str, ...]]
    starting_decklists: Mapping[str, tuple[str, ...]]
    table: tuple[TableCard, ...]
    life_totals: Mapping[str, int]

    def __post_init__(self) -> None:
        players = set(self.hands)
        if len(players) != 2 or any(set(m) != players for m in (
            self.libraries_in_draw_order, self.starting_decklists, self.life_totals
        )):
            raise ValueError("exactly two matching referee players required")
        if not self.objective:
            raise ValueError("missing objective")
        if any(type(v) is not int for v in self.life_totals.values()):
            raise ValueError("life totals must be integers, not booleans")
        for card in self.table:
            if card.controller not in players or card.owner not in players or not card.private_name:
                raise ValueError("invalid authoritative table card")
            if not card.face_down and not card.visible_name:
                raise ValueError("missing face-up visible identity")
        # External dictionary edits must not change authoritative referee state.
        for key in ("hands", "libraries_in_draw_order", "starting_decklists", "life_totals"):
            object.__setattr__(self, key, MappingProxyType(dict(getattr(self, key))))

    def actor_observation(self, actor: str) -> bytes:
        """Only approved fields; numeric life totals are referee supplied."""
        if actor not in self.hands:
            raise ValueError("unknown actor")
        opponent = next(p for p in self.hands if p != actor)
        own_table = []
        opponent_table = []
        for card in self.table:
            if card.owner == actor:
                if not card.face_down:
                    own_table.append({"appearance": "face_up", "card": card.visible_name})
                elif actor in card.actor_may_look_original:
                    own_table.append({"appearance": "face_down", "card": card.private_name})
                else:
                    own_table.append({"appearance": "face_down"})
            elif card.played_on_table:
                if card.face_down:
                    opponent_table.append({"appearance": "face_down"})
                else:
                    opponent_table.append({"appearance": "face_up", "card": card.visible_name})
        counts = Counter(self.starting_decklists[actor])
        observation = {
            "objective": self.objective,
            "own_cards": {
                "hand": list(self.hands[actor]),
                "decklist_multiset": [[name, counts[name]] for name in sorted(counts)],
                "table": own_table,
            },
            "opponent_table": opponent_table,
            "life_totals": {"self": self.life_totals[actor], "opponent": self.life_totals[opponent]},
        }
        validate_observation(observation)
        return json.dumps(observation, sort_keys=True, separators=(",", ":"), ensure_ascii=True).encode("ascii")


def validate_observation(payload: object) -> None:
    """Strict allowlist: reject extra fields and concealed card identities."""
    if not isinstance(payload, dict) or set(payload) != {"objective", "own_cards", "opponent_table", "life_totals"}:
        raise ValueError("invalid observation keys")
    if not isinstance(payload["objective"], str) or not payload["objective"]:
        raise ValueError("invalid objective")
    life = payload["life_totals"]
    if not isinstance(life, dict) or set(life) != {"self", "opponent"} or any(type(v) is not int for v in life.values()):
        raise ValueError("invalid referee life fields")
    own = payload["own_cards"]
    if not isinstance(own, dict) or set(own) != {"hand", "decklist_multiset", "table"}:
        raise ValueError("invalid own-card fields")
    if not isinstance(own["hand"], list) or any(not isinstance(x, str) or not x for x in own["hand"]):
        raise ValueError("invalid own hand")
    counts = own["decklist_multiset"]
    if not isinstance(counts, list) or any(not isinstance(x, list) or len(x) != 2 or
        not isinstance(x[0], str) or not x[0] or type(x[1]) is not int or x[1] <= 0 for x in counts):
        raise ValueError("invalid decklist counts")
    if [x[0] for x in counts] != sorted(set(x[0] for x in counts)):
        raise ValueError("decklist must be sorted and unique")
    if not isinstance(own["table"], list):
        raise ValueError("invalid own table")
    for card in own["table"]:
        if not isinstance(card, dict) or card.get("appearance") not in {"face_down", "face_up"}:
            raise ValueError("invalid own table card")
        if card.get("appearance") == "face_down" and set(card) == {"appearance"}:
            continue
        if set(card) != {"appearance", "card"} or not isinstance(card["card"], str) or not card["card"]:
            raise ValueError("invalid own visible identity")
    opp = payload["opponent_table"]
    if not isinstance(opp, list):
        raise ValueError("invalid opponent table")
    for card in opp:
        if not isinstance(card, dict) or card.get("appearance") not in {"face_down", "face_up"}:
            raise ValueError("invalid opponent card")
        if card["appearance"] == "face_down" and set(card) != {"appearance"}:
            raise ValueError("concealed opponent identity leak")
        if card["appearance"] == "face_up" and (set(card) != {"appearance", "card"} or
            not isinstance(card["card"], str) or not card["card"]):
            raise ValueError("invalid opponent visible card")


def _reject_duplicate_json_keys(pairs: list[tuple[str, object]]) -> dict:
    out = {}
    for key, value in pairs:
        if key in out:
            raise ValueError("duplicate JSON key")
        out[key] = value
    return out


class RefereeSession:
    """Owned by referee ONLY. Workers receive bytes from actor_observation."""
    def __init__(self, memory: RefereeMemory):
        self._state = memory
        self._version = 0

    def actor_observation(self, actor: str) -> bytes:
        return self._state.actor_observation(actor)

    def referee_apply_resolved_life(self, totals_from_forge: Mapping[str, int]) -> None:
        """Trusted referee only, after Forge resolves; never an actor command.

        A future Forge adapter must obtain totals from Player.getLife().
        """
        if set(totals_from_forge) != set(self._state.life_totals):
            raise ValueError("life update must include precisely all players")
        if any(type(n) is not int for n in totals_from_forge.values()):
            raise ValueError("life totals must be integers")
        self._state = replace(self._state, life_totals=dict(totals_from_forge))
        self._version += 1

    def actor_intent(self, actor: str, raw: bytes) -> bytes:
        """Only an inert 'pass' intent is permitted at this stage.

        A real Forge adapter must validate all gameplay actions and may never
        accept an actor-supplied authoritative life or game-state value.
        """
        if actor not in self._state.hands or len(raw) > 4096:
            raise ValueError("unknown actor or oversized command")
        try:
            data = json.loads(raw.decode("utf-8"), object_pairs_hook=_reject_duplicate_json_keys)
        except (UnicodeError, json.JSONDecodeError) as e:
            raise ValueError("invalid actor message") from e
        if data != {"intent": "pass"}:
            raise ValueError("unapproved actor command")
        return b'{"accepted":true}'
