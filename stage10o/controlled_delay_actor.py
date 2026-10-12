#!/usr/bin/env python3
"""Test-only independent Python actor for end-to-end referee deadline checks.

Responds only to exact allowlisted own/public observations. No hidden information,
raw Forge objects, process sharing, deck ordering, phase, priority, stack or timing
metadata are provided. 'slow' deliberately misses a short test-only deadline.
"""
import json
import math
import sys
import time


def unique_members(pairs):
    obj = {}
    for name, value in pairs:
        if name in obj:
            raise ValueError("duplicate key")
        obj[name] = value
    return obj


def invalid_constant(_value):
    raise ValueError("nonfinite JSON value")


def strict_float(token):
    value = float(token)
    if not math.isfinite(value):
        raise ValueError("nonfinite float")
    return value


if len(sys.argv) != 2 or sys.argv[1] not in ("slow", "prompt"):
    raise SystemExit("invalid test-only actor mode")

for line in sys.stdin.buffer:
    try:
        if not line.endswith(b"\n") or len(line) >= 16384:
            raise ValueError("invalid bounded observation frame")
        decoded = json.loads(line, object_pairs_hook=unique_members,
                             parse_constant=invalid_constant, parse_float=strict_float)
        if not isinstance(decoded, dict):
            raise ValueError("invalid observation")
        if "request" in decoded:
            if set(decoded) != {"request", "observation"} or decoded["request"] != "own_opening_hand":
                raise ValueError("unapproved request")
            own_view = decoded["observation"]
            opening = True
        else:
            own_view = decoded
            opening = False
        if not isinstance(own_view, dict) or set(own_view) != {
            "objective", "own_cards", "opponent_table", "life_totals"
        }:
            raise ValueError("unapproved observation field")
        if not isinstance(own_view["own_cards"], dict) or not isinstance(
                own_view["own_cards"].get("hand"), list):
            raise ValueError("invalid own hand")
        time.sleep(1.4 if sys.argv[1] == "slow" else 0.04)
        if opening:
            response = {"intent": "opening_hand", "keep": True,
                        "bottom_priority": list(range(len(own_view["own_cards"]["hand"])))}
        else:
            response = {"intent": "pass"}
        sys.stdout.write(json.dumps(response, separators=(",", ":")) + "\n")
        sys.stdout.flush()
    except (ValueError, TypeError, UnicodeError):
        raise SystemExit("rejected malformed actor input")
