"""Stage 8C target-aware public action features.

This is a development-only representation layered on top of the frozen Stage 8B
model. Forge resolves transient object IDs inside the referee boundary and emits
only public target descriptors. Raw object IDs are never learner features.
"""
from __future__ import annotations

import math
from collections import Counter

from stage8_ranker_features import ZONES, features as stage8b_features, public_context

TARGET_SEMANTICS_VERSION = "forge-public-targets-v2"
ALLOWED_TARGET_ZONES = set(ZONES) | {"unresolved", "player"}
ALLOWED_ROLES = {"self", "opponent", "public", "unknown"}


def _finite_number(value: str, key: str) -> float:
    try:
        number = float(value)
    except (TypeError, ValueError, OverflowError) as exc:
        raise ValueError(f"invalid target descriptor {key}") from exc
    if not math.isfinite(number):
        raise ValueError(f"invalid target descriptor {key}")
    return number


def parse_target_descriptor(text: str) -> dict:
    if not isinstance(text, str) or not text:
        raise ValueError("target descriptor must be a non-empty string")
    parts = text.split("|")
    if len(parts) < 3:
        raise ValueError("malformed target descriptor")
    prefix = {}
    for piece in parts[:2]:
        key, sep, value = piece.partition("=")
        if not sep or key not in ("zone", "role") or not value:
            raise ValueError("malformed target descriptor prefix")
        prefix[key] = value.lower()
    if prefix.get("zone") not in ALLOWED_TARGET_ZONES:
        raise ValueError("target descriptor uses an unknown public zone")
    if prefix.get("role") not in ALLOWED_ROLES:
        raise ValueError("target descriptor uses an unknown public role")

    if any(piece.lower().startswith("id=") for piece in parts):
        raise ValueError("raw object IDs may not become learner features")

    out = dict(prefix)
    out["name"] = parts[2].lower()
    if not out['name']:
        raise ValueError('target descriptor requires a name or opaque marker')
    seen = set()
    for piece in parts[3:]:
        key, sep, value = piece.partition("=")
        if not sep:
            raise ValueError("malformed target descriptor attribute")
        key = key.lower()
        if key in seen:
            raise ValueError('duplicate target descriptor attribute')
        seen.add(key)
        if key == "type":
            out[key] = value.lower()
        elif key in ("mv", "p", "t"):
            out[key] = _finite_number(value, key)
        elif key == "id":
            raise ValueError("raw object IDs may not become learner features")
        else:
            raise ValueError('unknown target descriptor attribute')
    if out['zone'] == 'player' and (out['name'] != '<player>' or len(parts) != 3):
        raise ValueError('player target must not contain card characteristics')
    if out['zone'] == 'unresolved' and (out['role'] != 'unknown' or out['name'] != '<opaque>' or len(parts) != 3):
        raise ValueError('unresolved target must remain opaque')
    if out['name'] in ('<face-down>', '<opaque>') and len(parts) != 3:
        raise ValueError('opaque target must not expose hidden characteristics')
    return out


def validated_target_descriptors(candidate: dict, required=False):
    if "target_public_semantics" not in candidate:
        if required or 'target_semantics_version' in candidate:
            raise ValueError("Stage 8C requires target_public_semantics from a fresh Forge capture")
        return None
    if candidate.get('target_semantics_version') != TARGET_SEMANTICS_VERSION:
        raise ValueError('obsolete or missing target_semantics_version; recapture typed Forge targets')
    values = candidate["target_public_semantics"]
    if not isinstance(values, list) or not all(isinstance(x, str) and x for x in values):
        raise ValueError("target_public_semantics must be a list of non-empty descriptors")

    return [parse_target_descriptor(x) for x in values]


def target_action_features(candidate: dict) -> dict:
    parsed = validated_target_descriptors(candidate, required=True)
    out = {"target_count": len(parsed) / 4.0} if parsed else {"target_count": 0.0}
    for key, count in Counter(d["zone"] for d in parsed).items():
        out["target_zone=" + key] = count / 4.0
    for key, count in Counter(d["role"] for d in parsed).items():
        out["target_role=" + key] = count / 4.0
    for key, count in Counter(d["name"] for d in parsed).items():
        out["target_name=" + key] = count / 4.0

    for d in parsed:
        for stat in ("mv", "p", "t"):
            out["target_" + stat] = out.get("target_" + stat, 0.0) + d.get(stat, 0.0) / 10.0
        for kind in ("land", "creature", "artifact", "enchantment", "instant", "sorcery", "planeswalker"):
            if kind in d.get("type", "").split():
                out["target_type=" + kind] = out.get("target_type=" + kind, 0.0) + 0.25
        if d["name"] in ("<face-down>", "<opaque>") or d["zone"] == "unresolved":
            out["target_opaque"] = out.get("target_opaque", 0.0) + 0.25
    return {k: v for k, v in out.items() if v}


def features(row: dict, candidate: dict) -> dict:
    """Stage 8B public semantics plus exact-target public semantics.

    Candidate-specific target facts are both direct features and interacted with
    the same legal public context. This lets the learner distinguish, for example,
    two same-name creatures with different current public power/toughness without
    exposing Forge object IDs.
    """
    base = dict(stage8b_features(row, candidate, "public_semantics"))
    target = target_action_features(candidate)
    context = public_context(row["public_state"], "public_semantics")
    for key, value in target.items():
        base[("target", key)] = value
        for state_key, state_value in context.items():
            base[("target_interaction", key, state_key)] = value * state_value
    return {k: v for k, v in base.items() if v}
