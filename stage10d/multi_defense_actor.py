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

    # This is a passive preference PLAN, never a privileged instruction.
    # No phase/turn/priority decision type is sent into the process.
    # The trusted Forge referee picks the first LEGAL item for its callback.
    lands = [i for i, name in enumerate(hand) if name in LANDS]
    lands.sort(key=lambda i: (hand[i] != 'Forest', hand[i] != 'Island', i))
    creatures = [i for i, name in enumerate(hand) if name in CREATURES]
    creatures.sort(key=lambda i: (hand[i] != 'Grizzly Bears', i))
    bears = [
        i for i, record in enumerate(view['own_cards']['table'])
        if record.get('appearance') == 'face_up'
        and record.get('card') == 'Grizzly Bears'
    ]
    # Conservative test-only play preference; no privileged callback state.
    if bears:
        creatures = []
    # A deck specializing in Llanowar Elves is the active attacking profile;
    # the opponent's distinct registered list uses its own creatures defensively.
    # These are test-only strategic preferences based solely on OUR decklist.
    own_counts = {
        pair[0]: pair[1] for pair in view['own_cards']['decklist_multiset']
        if isinstance(pair, list) and len(pair) == 2
    }
    active_attacks = bears[:8] if own_counts.get('Llanowar Elves', 0) >= 4 else []
    targets = [i for i, record in enumerate(view['opponent_table'])
               if record.get('appearance') == 'face_up'
               and record.get('card') == 'Grizzly Bears']
    # Passive distinct one-to-one preferences; trusted Forge checks blockers.
    block_pairs = [[own_i, opponent_i] for own_i, opponent_i
                   in zip(bears, targets)][:8]
    plan = {
        'intent': 'action_plan',
        'land_hand_indices': lands,
        'creature_hand_indices': creatures,
        'attack_table_indices': active_attacks,
        'block_pairs': block_pairs,
    }
    print(json.dumps(plan, separators=(',', ':')), flush=True)

