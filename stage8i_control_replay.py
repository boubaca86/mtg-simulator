#!/usr/bin/env python3
"""Stage 8I two-pass replay-equivalence tooling for bounded policy control.

Pass 1 records Forge's own complete-action choices. An external request file is
then generated from only decision_index + complete action identity. Pass 2 feeds
those requests through the bounded Java adapter. The comparison fails closed on
candidate drift, lifecycle drift, missing/extra requests, or any request that is
not exactly Forge's original selection.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import re
from pathlib import Path

from stage8d_shadow_policy import CAPTURE_PREFIX, load_checkpoint
from stage8e_returned_action_audit import RETURN_PREFIX
from stage8f_acceptance_audit import ACCEPT_PREFIX
from stage8h_lifecycle_audit import TERMINAL_PREFIX, audit_log as audit_lifecycle_log

CONTROL_PREFIX = "EXPERT_STAGE8I_CONTROL_SELECTION: "
RESULT_RE = re.compile(r" ended in \d+ ms\.")


def _payload(line: str, prefix: str) -> dict:
    value = json.loads(line[len(prefix):])
    if not isinstance(value, dict):
        raise ValueError("Stage 8I event must be a JSON object")
    return value


def _events(path: Path, prefix: str) -> list[dict]:
    return [
        _payload(line, prefix)
        for line in path.read_text().splitlines()
        if line.startswith(prefix)
    ]


def _result_signatures(path: Path) -> list[str]:
    return [
        RESULT_RE.sub(" ended in <ms> ms.", line)
        for line in path.read_text().splitlines()
        if line.startswith("Game Result: Game ")
    ]


def extract_requests(source: Path, output: Path) -> dict:
    captures = _events(source, CAPTURE_PREFIX)
    if not captures:
        raise ValueError("Stage 8I baseline has no Stage 8 captures")

    seen = set()
    rows: list[tuple[int, str]] = []
    for capture in captures:
        if capture.get("schema_version") != "stage8-capture-v1":
            raise ValueError("wrong Stage 8 capture schema")
        idx = capture.get("decision_index")
        selected = capture.get("selected_action")
        candidates = capture.get("candidates")
        if type(idx) is not int or idx < 0 or idx in seen:
            raise ValueError("invalid or duplicate baseline decision index")
        if not isinstance(selected, str) or not selected:
            raise ValueError("baseline selected action is missing")
        if not isinstance(candidates, list) or selected not in {
            c.get("action_identity") for c in candidates if isinstance(c, dict)
        }:
            raise ValueError("baseline selected action is not in captured candidates")
        seen.add(idx)
        rows.append((idx, selected))

    indices = [idx for idx, _ in rows]
    if indices != list(range(len(indices))):
        raise ValueError("baseline capture indices are not contiguous from zero")

    raw = "".join(
        f"{idx}\t{base64.urlsafe_b64encode(identity.encode()).decode()}\n"
        for idx, identity in rows
    ).encode()
    output.write_bytes(raw)
    return {
        "source_log": source.name,
        "requests": len(rows),
        "request_file": output.name,
        "request_file_sha256": hashlib.sha256(raw).hexdigest(),
        "fields": ["decision_index", "complete_action_identity"],
        "hidden_information_fields": 0,
    }


def load_requests(path: Path) -> tuple[dict[int, str], str]:
    raw = path.read_bytes()
    requests: dict[int, str] = {}
    for line in raw.decode().splitlines():
        if not line:
            continue
        parts = line.split("\t")
        if len(parts) != 2:
            raise ValueError("invalid Stage 8I request line")
        idx = int(parts[0])
        identity = base64.urlsafe_b64decode(parts[1].encode()).decode()
        if idx < 0 or not identity or idx in requests:
            raise ValueError("invalid or duplicate Stage 8I request")
        requests[idx] = identity
    if not requests:
        raise ValueError("empty Stage 8I request file")
    if list(requests) != list(range(len(requests))):
        raise ValueError("Stage 8I request indices are not contiguous from zero")
    return requests, hashlib.sha256(raw).hexdigest()


def _assert_event_sequence_equal(baseline: Path, controlled: Path, prefix: str, name: str) -> int:
    left = _events(baseline, prefix)
    right = _events(controlled, prefix)
    if left != right:
        raise ValueError(f"Stage 8I {name} sequence drift")
    return len(left)


def compare_pair(checkpoint, baseline: Path, controlled: Path, request_file: Path,
                 expected_games: int) -> dict:
    # Whole-log safety/lifecycle validation runs before the equivalence checks.
    base_audit = audit_lifecycle_log(baseline, expected_games, checkpoint)
    controlled_audit = audit_lifecycle_log(controlled, expected_games, checkpoint)

    requests, request_sha = load_requests(request_file)
    baseline_captures = _events(baseline, CAPTURE_PREFIX)
    controlled_captures = _events(controlled, CAPTURE_PREFIX)
    if baseline_captures != controlled_captures:
        raise ValueError("Stage 8I complete capture sequence drift")
    if len(baseline_captures) != len(requests):
        raise ValueError("Stage 8I request count differs from baseline captures")

    controls = _events(controlled, CONTROL_PREFIX)
    if len(controls) != len(requests):
        raise ValueError("Stage 8I not every external request was consumed exactly once")
    if _events(baseline, CONTROL_PREFIX):
        raise ValueError("Stage 8I baseline unexpectedly used the control adapter")

    seen = set()
    for event in controls:
        if (event.get("schema_version") != "stage8i-control-selection-v1"
                or event.get("requested_action_in_candidates") is not True
                or event.get("same_as_forge") is not True
                or event.get("forge_referee") is not True
                or event.get("promotion_allowed") is not False):
            raise ValueError("invalid Stage 8I control-selection event")
        idx = event.get("decision_index")
        if type(idx) is not int or idx not in requests or idx in seen:
            raise ValueError("invalid/duplicate Stage 8I control decision index")
        if event.get("requested_action") != requests[idx]:
            raise ValueError("Stage 8I adapter consumed a different request identity")
        if event.get("forge_selected_action") != requests[idx]:
            raise ValueError("Stage 8I replay request differs from Forge baseline choice")
        if event.get("request_file_sha256") != request_sha:
            raise ValueError("Stage 8I request-file digest drift")
        if event.get("request_count") != len(requests):
            raise ValueError("Stage 8I Java adapter request-count drift")
        if type(event.get("candidate_count")) is not int or event["candidate_count"] < 1:
            raise ValueError("invalid Stage 8I candidate count")
        seen.add(idx)

    if seen != set(requests):
        raise ValueError("Stage 8I request coverage is incomplete")

    returned = _assert_event_sequence_equal(
        baseline, controlled, RETURN_PREFIX, "returned-action"
    )
    accepted = _assert_event_sequence_equal(
        baseline, controlled, ACCEPT_PREFIX, "controller-acceptance"
    )
    terminals = _assert_event_sequence_equal(
        baseline, controlled, TERMINAL_PREFIX, "terminal-lifecycle"
    )
    if _result_signatures(baseline) != _result_signatures(controlled):
        raise ValueError("Stage 8I game-result drift")

    # The independent Stage 8H audits must agree on the terminal aggregate too.
    base_terminal = base_audit["terminal_outcome_counts"]
    controlled_terminal = controlled_audit["terminal_outcome_counts"]
    if base_terminal != controlled_terminal:
        raise ValueError("Stage 8I terminal outcome aggregate drift")

    return {
        "baseline_log": baseline.name,
        "controlled_log": controlled.name,
        "request_file": request_file.name,
        "request_file_sha256": request_sha,
        "requests": len(requests),
        "control_events": len(controls),
        "captures_reproduced_exactly": len(baseline_captures),
        "returned_actions_reproduced_exactly": returned,
        "controller_acceptances_reproduced_exactly": accepted,
        "terminal_events_reproduced_exactly": terminals,
        "game_results_reproduced_exactly": len(_result_signatures(baseline)),
        "invalid_or_uncaptured_requests_accepted": 0,
        "different_from_forge_requests_accepted": 0,
        "forge_referee": True,
        "learned_policy_controlled_gameplay": False,
        "promotion_allowed": False,
    }


def aggregate(pairs: list[dict]) -> dict:
    keys = (
        "requests", "control_events", "captures_reproduced_exactly",
        "returned_actions_reproduced_exactly",
        "controller_acceptances_reproduced_exactly",
        "terminal_events_reproduced_exactly",
        "game_results_reproduced_exactly",
        "invalid_or_uncaptured_requests_accepted",
        "different_from_forge_requests_accepted",
    )
    totals = {key: sum(pair[key] for pair in pairs) for key in keys}
    return {
        "schema_version": "stage8i-bounded-control-replay-v1",
        "mode": "external-request-replay-equivalence",
        "promotion_allowed": False,
        "forge_referee": True,
        "learned_policy_controlled_gameplay": False,
        "require_forge_match": True,
        "totals": totals,
        "pairs": pairs,
        "gate_passed": (
            totals["requests"] > 0
            and totals["control_events"] == totals["requests"]
            and totals["captures_reproduced_exactly"] == totals["requests"]
            and totals["invalid_or_uncaptured_requests_accepted"] == 0
            and totals["different_from_forge_requests_accepted"] == 0
        ),
        "limitations": [
            "Stage 8I deliberately requests Forge's own selected actions; it is not learned-policy strength evidence.",
            "The adapter cannot invent or mutate actions: it can reference only complete candidates captured by Forge.",
            "Pass is not yet represented as a learned action.",
            "A later stage must predeclare fresh seeds and safety gates before allowing the learned policy to choose a different legal action.",
        ],
    }


def main():
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)

    extract = sub.add_parser("extract")
    extract.add_argument("source", type=Path)
    extract.add_argument("--output", required=True, type=Path)

    compare = sub.add_parser("compare")
    compare.add_argument("checkpoint", type=Path)
    compare.add_argument("--baseline", nargs="+", required=True, type=Path)
    compare.add_argument("--controlled", nargs="+", required=True, type=Path)
    compare.add_argument("--requests", nargs="+", required=True, type=Path)
    compare.add_argument("--expected-games", type=int, default=4)
    compare.add_argument("--output", required=True, type=Path)

    args = parser.parse_args()
    if args.command == "extract":
        print(json.dumps(extract_requests(args.source, args.output), sort_keys=True))
        return

    if not (len(args.baseline) == len(args.controlled) == len(args.requests)):
        raise ValueError("Stage 8I baseline/controlled/request lists must have equal length")
    policy = load_checkpoint(args.checkpoint)
    pairs = [
        compare_pair(policy, baseline, controlled, request, args.expected_games)
        for baseline, controlled, request in zip(args.baseline, args.controlled, args.requests)
    ]
    report = aggregate(pairs)
    args.output.write_text(json.dumps(report, sort_keys=True, indent=2) + "\n")
    print(json.dumps({
        "gate_passed": report["gate_passed"],
        "requests": report["totals"]["requests"],
        "returned_actions": report["totals"]["returned_actions_reproduced_exactly"],
        "terminal_events": report["totals"]["terminal_events_reproduced_exactly"],
        "promotion_allowed": report["promotion_allowed"],
    }, sort_keys=True))


if __name__ == "__main__":
    main()
