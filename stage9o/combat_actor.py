#!/usr/bin/env python3
"""Stage9O deliberately simple external actor: no referee, phase, turn or stack input."""
import json
import sys

LANDS = frozenset(('Forest', 'Island', 'Plains', 'Swamp', 'Mountain', 'Wastes'))
CREATURES = frozenset(('Llanowar Elves', 'Grizzly Bears'))

for raw in sys.stdin.buffer:
    if len(raw) > 16384:
        raise SystemExit('oversized observation')
    view = json.loads(raw)
    # Strict denylist: an actor must never be told game authority fields.
    if set(view) != {'objective', 'own_cards', 'opponent_table', 'life_totals'}:
        raise SystemExit('observation contract violation')
    if set(view['own_cards']) != {'hand', 'decklist_multiset', 'table'}:
        raise SystemExit('own card projection violation')
    hand = view['own_cards']['hand']
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
