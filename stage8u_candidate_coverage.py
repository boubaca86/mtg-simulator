"""Stage 8U read-only candidate-coverage contract.

Validates *observed* public legal-candidate telemetry; never constructs legal moves,
executes Forge, or assigns outcomes to unselected candidates.
"""
from __future__ import annotations
import hashlib
import json
from collections import Counter

SCHEMA = "stage8u-public-legal-candidates-v1"
FORBIDDEN = frozenset({"opponent_hand", "opponent_library", "future_draws",
                       "hidden_world", "sampled_world", "opponent_private_cards"})
ALLOWED_ROLES = frozenset({"self", "opponent", "public", "unknown"})
ALLOWED_EFFECTS = frozenset({"gain_life", "pump", "damage", "draw", "destroy",
                             "counter", "tap", "untap", "other", "none"})


def validate_event(event: dict) -> dict:
    if not isinstance(event, dict) or event.get("schema_version") != SCHEMA:
        raise ValueError("Unsupported candidate-coverage schema")
    if FORBIDDEN.intersection(event):
        raise ValueError("Private-information field prohibited")
    if not isinstance(event.get("run_seed"), int) or isinstance(event["run_seed"], bool):
        raise ValueError("Missing integer run seed")
    if not isinstance(event.get("decision_index"), int) or event["decision_index"] < 0:
        raise ValueError("Invalid decision index")
    candidates = event.get("legal_candidates")
    if not isinstance(candidates, list) or not candidates:
        raise ValueError("Missing Forge-legal candidate list")
    identities = set()
    for candidate in candidates:
        if not isinstance(candidate, dict) or set(candidate) != {"identity", "targets", "effect"}:
            raise ValueError("Unexpected candidate fields")
        identity = candidate["identity"]
        if not isinstance(identity, str) or not identity or identity in identities:
            raise ValueError("Missing or duplicate legal identity")
        identities.add(identity)
        if candidate["effect"] not in ALLOWED_EFFECTS:
            raise ValueError("Unknown public effect")
        if not isinstance(candidate["targets"], list):
            raise ValueError("Invalid targets")
        for target in candidate["targets"]:
            if not isinstance(target, dict) or set(target) != {"role", "kind"}:
                raise ValueError("Unexpected target fields")
            if target["role"] not in ALLOWED_ROLES or target["kind"] not in ("card", "player", "spell"):
                raise ValueError("Invalid public target")
    selected = event.get("selected_identity")
    if not isinstance(selected, str) or selected not in identities:
        raise ValueError("Selected action absent from Forge-legal candidates")
    if event.get("legal_candidate_count") != len(candidates):
        raise ValueError("Candidate count mismatch")
    if set(event) != {"schema_version", "run_seed", "decision_index", "legal_candidates",
                      "selected_identity", "legal_candidate_count"}:
        raise ValueError("Unknown event fields")
    return event


def coverage(events: list[dict]) -> dict:
    seen = set()
    available, selected = Counter(), Counter()
    for event in events:
        validate_event(event)
        key = event["run_seed"], event["decision_index"]
        if key in seen:
            raise ValueError("Duplicate decision")
        seen.add(key)
        for candidate in event["legal_candidates"]:
            targets = candidate["targets"] or [{"role": "unknown", "kind": "spell"}]
            for target in targets:
                feature = candidate["effect"] + "_to_" + target["role"] + "_" + target["kind"]
                available[feature] += 1
                if candidate["identity"] == event["selected_identity"]:
                    selected[feature] += 1
    return {"schema_version": SCHEMA, "decisions": len(seen),
            "available": dict(sorted(available.items())),
            "selected": dict(sorted(selected.items()))}


def report_bytes(events: list[dict]) -> bytes:
    payload = coverage(events)
    return (json.dumps(payload, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")


def report_sha256(events: list[dict]) -> str:
    return hashlib.sha256(report_bytes(events)).hexdigest()
