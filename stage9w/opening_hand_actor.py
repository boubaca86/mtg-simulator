#!/usr/bin/env python3
"""Stage9O deliberately simple external actor: no referee, phase, turn or stack input."""
import json
import sys

LANDS = frozenset(('Forest', 'Island', 'Plains', 'Swamp', 'Mountain', 'Wastes'))
CREATURES = frozenset(('Llanowar Elves', 'Grizzly Bears'))

for raw in sys.stdin.buffer:
    if len(raw) > 16384:
        raise SystemExit('oversized observation')
    message = json.loads(raw)
    opening = isinstance(message, dict) and set(message) == {'request', 'observation'}
    if opening:
        if message['request'] != 'own_opening_hand':
            raise SystemExit('unapproved IPC action type')
        view = message['observation']
    else:
        view = message
    # Strict denylist: an actor must never be told game authority fields.
    if set(view) != {'objective', 'own_cards', 'opponent_table', 'life_totals'}:
        raise SystemExit('observation contract violation')
    if set(view['own_cards']) != {'hand', 'decklist_multiset', 'table'}:
        raise SystemExit('own card projection violation')
    hand = view['own_cards']['hand']
    if opening:
        # Derive everything from OWN hand; no deck order, opponent hand, or
        # referee mulligan counter is sent. The referee validates the index
        # permutation and uses its own rules to choose how many to bottom.
        lands = sum(1 for name in hand if name in LANDS)
        keep = len(hand) <= 1 or 2 <= lands <= 5
        # First choose redundant duplicate cards, then excess creatures,
        # then lands; stable ordering breaks ties without any hidden inputs.
        def bottom_score(index):
            name = hand[index]
            if name in LANDS:
                return (2, index)
            if name in CREATURES:
                return (1, index)
            return (0, index)
        bottom_priority = sorted(range(len(hand)), key=bottom_score)
        print(json.dumps({
            'intent': 'opening_hand',
            'keep': keep,
            'bottom_priority': bottom_priority,
        }, separators=(',', ':')), flush=True)
        continue

    response = {'intent': 'pass'}
    for index, name in enumerate(hand):
        if name in LANDS:
            response = {'intent': 'play_land', 'hand_index': index}
            break
    else:
        for index, name in enumerate(hand):
            if name in CREATURES:
                response = {'intent': 'cast_creature', 'hand_index': index}
                break
        else:
            indices = [
                i for i, record in enumerate(view['own_cards']['table'])
                if record.get('appearance') == 'face_up'
                and record.get('card') == 'Grizzly Bears'
            ]
            if indices:
                response = {'intent': 'attack', 'table_indices': indices[:8]}
    print(json.dumps(response, separators=(',', ':')), flush=True)
