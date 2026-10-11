#!/usr/bin/env python3
import json
import sys

for raw in sys.stdin.buffer:
    if len(raw) > 16384:
        raise SystemExit('oversized message')
    view = json.loads(raw)
    hand = view['own_cards']['hand']
    intent = {'intent': 'pass'}
    for index, name in enumerate(hand):
        if name in ('Forest', 'Island', 'Plains', 'Swamp', 'Mountain', 'Wastes'):
            intent = {'intent': 'play_land', 'hand_index': index}
            break
    else:
        for index, name in enumerate(hand):
            if name in ('Llanowar Elves', 'Grizzly Bears'):
                intent = {'intent': 'cast_creature', 'hand_index': index}
                break
    print(json.dumps(intent), flush=True)
