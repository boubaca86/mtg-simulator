#!/usr/bin/env python3
"""Stage9O deliberately simple external actor: no referee, phase, turn or stack input."""
import json
import sys
import math


def _strict_object_pairs(pairs):
    """Reject duplicate decoded JSON member names at every nesting level."""
    result = {}
    for key, value in pairs:
        if key in result:
            raise SystemExit('duplicate JSON field')
        result[key] = value
    return result


def _reject_nonfinite_constant(value):
    raise SystemExit('nonfinite JSON literal')


def _strict_json_float(token):
    result = float(token)
    if not math.isfinite(result):
        raise SystemExit('nonfinite JSON number')
    return result

LANDS = frozenset(('Forest', 'Island', 'Plains', 'Swamp', 'Mountain', 'Wastes'))
CREATURES = frozenset(('Llanowar Elves', 'Grizzly Bears'))

for raw in sys.stdin.buffer:
    if len(raw) > 16384:
        raise SystemExit('oversized observation')
    message = json.loads(raw, object_pairs_hook=_strict_object_pairs,
                         parse_constant=_reject_nonfinite_constant,
                         parse_float=_strict_json_float)
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
    if not isinstance(view['objective'], str):
        raise SystemExit('invalid objective')
    if not isinstance(view['life_totals'], dict) or set(view['life_totals']) != {'self', 'opponent'}:
        raise SystemExit('invalid life projection')
    if any(type(value) is not int for value in view['life_totals'].values()):
        raise SystemExit('invalid life totals')
    for key in ('table',):
        if not isinstance(view['own_cards'][key], list):
            raise SystemExit('invalid own table')
    if not isinstance(view['opponent_table'], list):
        raise SystemExit('invalid opponent table')
    for record in view['own_cards']['table'] + view['opponent_table']:
        if not isinstance(record, dict) or record.get('appearance') not in ('face_up', 'face_down'):
            raise SystemExit('invalid card appearance')
        if record['appearance'] == 'face_down':
            if set(record) != {'appearance'}:
                raise SystemExit('face-down identity forbidden')
        elif set(record) != {'appearance', 'card'} or not isinstance(record['card'], str):
            raise SystemExit('invalid face-up card')
    if not isinstance(view['own_cards']['hand'], list) or any(not isinstance(x, str) for x in view['own_cards']['hand']):
        raise SystemExit('invalid own hand')
    if not isinstance(view['own_cards']['decklist_multiset'], list):
        raise SystemExit('invalid own deck multiset')
    for entry in view['own_cards']['decklist_multiset']:
        if not isinstance(entry, list) or len(entry) != 2 or not isinstance(entry[0], str) or type(entry[1]) is not int or entry[1] < 0:
            raise SystemExit('invalid own deck entry')
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
    # Prefer distinct one-to-one blocks: an ordinary Bear cannot block two
    # separate creatures. The referee checks the actual combat legality.
    # Opponent's visible single creature may require multiple blockers
    # (e.g. menace). This is only a preference: Forge decides legality.
    # We never request an attacker list, keyword data, phase, or hidden zone.
    if len(targets) == 1 and len(bears) >= 2:
        block_pairs = [[own_i, targets[0]] for own_i in bears[:2]]
    else:
        block_pairs = [[own_i, enemy_i] for own_i, enemy_i
                       in zip(bears, targets)][:8]
    plan = {
        'intent': 'action_plan',
        'land_hand_indices': lands,
        'creature_hand_indices': creatures,
        'attack_table_indices': active_attacks,
        'block_pairs': block_pairs,
    }
    print(json.dumps(plan, separators=(',', ':')), flush=True)

