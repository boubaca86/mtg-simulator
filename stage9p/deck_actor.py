#!/usr/bin/env python3
"""Stage9P deliberately simple external actor: no referee, phase, turn or stack input."""
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
    decklist = view['own_cards']['decklist_multiset']
    if not isinstance(decklist, list) or any(
        not isinstance(entry, list) or len(entry) != 2
        or not isinstance(entry[0], str) or not entry[0]
        or not isinstance(entry[1], int) or isinstance(entry[1], bool)
        or entry[1] <= 0 for entry in decklist
    ):
        raise SystemExit('invalid registered starting deck multiset')
    if [entry[0] for entry in decklist] != sorted(set(entry[0] for entry in decklist)):
        raise SystemExit('registered multiset not canonical sorted unique')
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
