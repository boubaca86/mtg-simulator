"""Stage 8Q public-only target/effect interactions (development, not a controller).

The Stage 8O policy scored target ownership and ability words separately. This
module creates explicit *interactions*, e.g. fixed damage aimed at an opponent
versus the same damage aimed at one's own public creature. Nothing here claims
an action is illegal, deals damage successfully, or has a guaranteed payoff.

No new reserved evaluation, action dispatch, training or card-rule override.
"""
from __future__ import annotations

import math
import re

from stage8c_target_features import parse_target_descriptor
from stage8o_outcome_policy import features as frozen_features, validate_input
from stage8_ranker_features import canonical_action

VERSION = "stage8q-public-target-effect-v1"
TARGET_KINDS = ("opponent_player", "self_player", "opponent_creature",
                "self_creature", "other")
EFFECTS = ("damage", "gain_life", "pump", "destroy")
DAMAGE_AMOUNT = re.compile(r"\bdeals?\s+(\d+)\s+damage\b", re.IGNORECASE)
LIFE_GAIN = re.compile(r"\bgains?\s+\d+\s+life\b", re.IGNORECASE)
PUMP = re.compile(r"\bput\s+(?:a|one)\s+\+1/\+1\s+counter\b|"
                  r"\+\d+/\+\d+|\bgains?\s+(?:indestructible|hexproof|lifelink)\b",
                  re.IGNORECASE)
DESTROY = re.compile(r"\bdestroy\b", re.IGNORECASE)


def _target_kind(target: dict) -> str:
    zone, role = target["zone"], target["role"]
    # Contradictory public ownership is unsafe to use as a strategic feature.
    if zone == "own_battlefield" and role == "opponent":
        raise ValueError("inconsistent public target ownership")
    if zone == "opponent_battlefield" and role == "self":
        raise ValueError("inconsistent public target ownership")
    if zone == "player" and role in ("self", "opponent"):
        return role + "_player"
    if zone in ("own_battlefield", "opponent_battlefield") and role in ("self", "opponent"):
        if "creature" in target.get("type", "").lower().split():
            return role + "_creature"
    return "other"


def _ability(identity: str, actor_name: str) -> str:
    canonical = canonical_action(identity, actor_name)
    # Exactly one canonical ability field, independent of raw object IDs.
    if not canonical.startswith("ability=") or "|x=" not in canonical:
        raise ValueError("invalid canonical action")
    return canonical[8:canonical.index("|x=")]


def features(public_model_input: dict, candidate: dict) -> dict[str, float]:
    """Fixed numerical schema, safe for future *development* training only.

    Every candidate must be one of Forge's public legal alternatives. Stage 8O
    and 8C validators jointly enforce the public-input and typed-target contract.
    """
    safe = validate_input(public_model_input)
    base = frozen_features(safe, candidate)
    selected = next((c for c in safe["candidates"] if c == candidate), None)
    if selected is None:
        raise ValueError("candidate outside the public legal set")
    actor = safe["public_state"]["acting_player_name"]
    ability = _ability(selected["action_identity"], actor)
    targets = [parse_target_descriptor(t) for t in selected["target_public_semantics"]]
    kinds = [_target_kind(t) for t in targets]
    # For a single public player target, cross-check the normalized recipe
    # against the target descriptor. Forge's canonical recipe labels the two
    # players SELF/OPPONENT, independent of player/deck names.
    if len(targets) == 1 and targets[0]["zone"] == "player":
        recipe = canonical_action(selected["action_identity"], actor)
        match = re.search(r"\|targets=\[([^\]]+)\]\|choices=", recipe)
        if match and match.group(1) in ("self", "opponent"):
            if match.group(1) != targets[0]["role"]:
                raise ValueError("public target descriptor disagrees with player action identity")
    damages = [int(x) for x in DAMAGE_AMOUNT.findall(ability)]
    # Multiple fixed damage clauses and X costs require a richer referee-backed
    # parser; do not pretend their amount is known here.
    fixed_damage = len(damages) == 1
    amount = min(damages[0] / 10.0, 3.0) if fixed_damage else 0.0
    effects = {
        "damage": float(bool(re.search(r"\bdamage\b", ability))),
        "gain_life": float(bool(LIFE_GAIN.search(ability))),
        "pump": float(bool(PUMP.search(ability))),
        "destroy": float(bool(DESTROY.search(ability))),
    }
    result = dict(base)
    result["q_fixed_damage_amount_known"] = float(fixed_damage)
    result["q_fixed_damage_amount"] = amount
    for effect in EFFECTS:
        result[f"q_effect_{effect}"] = effects[effect]
        for kind in TARGET_KINDS:
            # Counts, not target names or opaque Forge object identifiers.
            result[f"q_{effect}_to_{kind}"] = min(3.0, effects[effect] * kinds.count(kind) / 4.0)
    for kind in TARGET_KINDS:
        result[f"q_target_{kind}"] = min(3.0, kinds.count(kind) / 4.0)
        result[f"q_fixed_damage_to_{kind}"] = min(3.0, amount * kinds.count(kind))
    for role in ("self", "opponent"):
        creatures = [t for t, k in zip(targets, kinds) if k == f"{role}_creature"]
        result[f"q_{role}_creature_public_power"] = min(3.0, sum(t.get("p", 0) for t in creatures) / 10.0)
        result[f"q_{role}_creature_public_toughness"] = min(3.0, sum(t.get("t", 0) for t in creatures) / 10.0)
        # Not 'lethal': replacement/prevention/indestructible remain Forge rules.
        result[f"q_{role}_creature_damage_vs_toughness"] = float(bool(
            fixed_damage and any("t" in t and damages[0] >= t["t"] for t in creatures)))
    if not all(type(x) in (float, int) and math.isfinite(x) and -3 <= x <= 3
               for x in result.values()):
        raise ValueError("nonfinite or unbounded public target-effect feature")
    return result
