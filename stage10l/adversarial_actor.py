#!/usr/bin/env python3
"""Test-only adversarial actor: deliberately hangs after one legal observation.

Receives the same public/own-card observation envelope as other actors.
Never gets access to referee state, hidden cards, turn, priority or stack.
One process can simulate either a completely silent or unterminated reply.
"""
import sys
import time

if len(sys.argv) != 2 or sys.argv[1] not in ("silent", "partial", "noread"):
    raise SystemExit("invalid adversarial actor mode")

if sys.argv[1] == "noread":
    # Real child process deliberately keeps stdin open but never drains it.
    while True:
        time.sleep(60)

for raw in sys.stdin.buffer:
    if len(raw) > 16384:
        raise SystemExit("oversized observation")
    if sys.argv[1] == "partial":
        sys.stdout.buffer.write(b'{"intent":"pass"}')
        sys.stdout.buffer.flush()
    while True:
        time.sleep(60)
