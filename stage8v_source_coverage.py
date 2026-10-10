"""Validate non-exhaustive AI-filtered candidate counts from the Stage 8V Java helper."""
from __future__ import annotations
import json
import re

SCHEMA = "stage8v-ai-filtered-top-level-v2"
SOURCE = "SpellAbilityPicker.getCandidateSpellsAndAbilities"
KEYS = {
    "schema_version", "source", "complete_legal_enumeration",
    "target_combinations_enumerated", "priority_pass_enumerated",
    "selected_status", "session_id", "matchup_id", "orientation", "run_seed", "decision_index",
    "top_level_candidate_count", "proposed_candidate_index",
}


def validate_event(event: dict) -> dict:
    if not isinstance(event, dict) or set(event) != KEYS:
        raise ValueError("Unexpected Stage 8V event fields")
    if event["schema_version"] != SCHEMA or event["source"] != SOURCE:
        raise ValueError("Unrecognized candidate source")
    if event["complete_legal_enumeration"] is not False:
        raise ValueError("Stage 8V is not a complete legal-action enumerator")
    if event["target_combinations_enumerated"] is not False:
        raise ValueError("Stage 8V does not enumerate target combinations")
    if event["priority_pass_enumerated"] is not False:
        raise ValueError("Stage 8V does not enumerate priority passes")
    if event["selected_status"] != "proposed_before_execution":
        raise ValueError("Stage 8V cannot assert actual execution")
    if not isinstance(event["session_id"], str) or not re.fullmatch(r"[0-9a-f]{32}", event["session_id"]) or event["session_id"] == "0" * 32:
        raise ValueError("Invalid game session identifier")
    if not isinstance(event["matchup_id"], str) or not re.fullmatch(r"[A-Za-z0-9._:/-]{1,96}", event["matchup_id"]) or event["matchup_id"] == "unspecified":
        raise ValueError("Explicit matchup provenance required")
    if not isinstance(event["orientation"], str) or not re.fullmatch(r"[a-z][a-z0-9-]{1,47}", event["orientation"]):
        raise ValueError("Explicit orientation provenance required")
    for key in ("run_seed", "decision_index", "top_level_candidate_count",
                "proposed_candidate_index"):
        if type(event[key]) is not int:
            raise ValueError("Invalid integer event field")
    if event["decision_index"] < 0 or event["top_level_candidate_count"] < 1:
        raise ValueError("Invalid decision or candidate count")
    if not 0 <= event["proposed_candidate_index"] < event["top_level_candidate_count"]:
        raise ValueError("Proposed candidate index is out of range")
    return event


def parse_log(lines: list[str]) -> list[dict]:
    prefix = "EXPERT_STAGE8V_TOP_LEVEL: "
    events = []
    seen = set()
    provenance = {}
    for line in lines:
        if not line.startswith(prefix):
            continue
        event = validate_event(json.loads(line[len(prefix):]))
        session = event["session_id"]
        identity = (event["run_seed"], event["matchup_id"], event["orientation"])
        if session in provenance and provenance[session] != identity:
            raise ValueError("Game session provenance changed")
        provenance[session] = identity
        key = (session, event["decision_index"])
        if key in seen:
            raise ValueError("Duplicate Stage 8V decision")
        seen.add(key)
        events.append(event)
    return events


def summarize(events: list[dict]) -> dict:
    if not events:
        raise ValueError("No Stage 8V events")
    seen = set()
    counts = []
    provenance = {}
    for event in events:
        validate_event(event)
        key = (event["run_seed"], event["decision_index"])
        if key in seen:
            raise ValueError("Duplicate Stage 8V decision")
        seen.add(key)
        session = event["session_id"]
        identity = (event["run_seed"], event["matchup_id"], event["orientation"])
        if session in provenance and provenance[session] != identity:
            raise ValueError("Game session provenance changed")
        provenance[session] = identity
        counts.append(event["top_level_candidate_count"])
    return {
        "schema_version": SCHEMA,
        "decisions": len(counts),
        "sessions": len(provenance),
        "candidate_count_min": min(counts),
        "candidate_count_max": max(counts),
        "candidate_count_mean": sum(counts) / len(counts),
        "complete_legal_enumeration": False,
        "gameplay_strength_evidence": False,
    }
