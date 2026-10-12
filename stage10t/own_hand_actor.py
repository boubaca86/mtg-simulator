#!/usr/bin/env python3
"""Stage10T: ordinary plans plus bounded own-hand preferences; no referee state."""
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

def validate_view(view):
    if not isinstance(view, dict) or set(view) != {'objective', 'own_cards', 'opponent_table', 'life_totals'}:
        raise SystemExit('observation contract violation')
    if not isinstance(view['objective'], str) or not 0 < len(view['objective']) <= 2000:
        raise SystemExit('invalid objective')
    own = view['own_cards']
    if not isinstance(own, dict) or set(own) != {'hand', 'decklist_multiset', 'table'}:
        raise SystemExit('own card projection violation')
    def name(value):
        return isinstance(value, str) and 0 < len(value) <= 200
    if not isinstance(own['hand'], list) or len(own['hand']) > 128 or not all(map(name, own['hand'])):
        raise SystemExit('invalid own hand')
    counts = own['decklist_multiset']
    if not isinstance(counts, list) or len(counts) > 256:
        raise SystemExit('invalid registered deck')
    seen = set()
    for pair in counts:
        if (not isinstance(pair, list) or len(pair) != 2 or not name(pair[0])
                or type(pair[1]) is not int or not 1 <= pair[1] <= 1000 or pair[0] in seen):
            raise SystemExit('invalid registered deck')
        seen.add(pair[0])
    for table in (own['table'], view['opponent_table']):
        if not isinstance(table, list) or len(table) > 128:
            raise SystemExit('invalid visible table')
        for card in table:
            if not isinstance(card, dict):
                raise SystemExit('invalid visible card')
            if card.get('appearance') == 'face_down':
                if set(card) != {'appearance'}:
                    raise SystemExit('face-down information leak')
            elif card.get('appearance') == 'face_up':
                if set(card) != {'appearance', 'card'} or not name(card['card']):
                    raise SystemExit('invalid visible card')
            else:
                raise SystemExit('invalid appearance')
    life = view['life_totals']
    if (not isinstance(life, dict) or set(life) != {'self', 'opponent'}
            or any(type(n) is not int or abs(n) > 1000000000 for n in life.values())):
        raise SystemExit('invalid life totals')


def own_hand_order(hand):
    # Test policy only: keep two basic lands (prefer Forest); discard unsupported
    # spells then surplus lands before useful creatures. Only OWN hand is used.
    lands = [i for i, card in enumerate(hand) if card in LANDS]
    lands.sort(key=lambda i: (hand[i] != 'Forest', i))
    retained_lands = set(lands[:2])
    def score(i):
        card = hand[i]
        if card not in LANDS and card not in CREATURES:
            return (0, i)
        if card in LANDS and i not in retained_lands:
            return (1, i)
        if card == 'Llanowar Elves':
            return (2, i)
        if card == 'Grizzly Bears':
            return (3, i)
        return (4, i)
    return sorted(range(len(hand)), key=score)


while True:
    raw = sys.stdin.buffer.readline(16385)
    if not raw:
        break
    if len(raw) > 16384 or not raw.endswith(b'\n'):
        raise SystemExit('oversized or unterminated observation')
    message = json.loads(raw, object_pairs_hook=_strict_object_pairs,
                         parse_constant=_reject_nonfinite_constant,
                         parse_float=_strict_json_float)
    envelope = isinstance(message, dict) and set(message) == {'request', 'observation'}
    kind = message['request'] if envelope else None
    if envelope and kind not in ('own_opening_hand', 'own_hand_order'):
        raise SystemExit('unapproved IPC action type')
    view = message['observation'] if envelope else message
    validate_view(view)
    hand = view['own_cards']['hand']
    if kind == 'own_hand_order':
        print(json.dumps({'intent': 'hand_order', 'hand_priority': own_hand_order(hand)},
                         separators=(',', ':')), flush=True)
        continue
    opening = kind == 'own_opening_hand'
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
    plan = {
        'intent': 'action_plan',
        'land_hand_indices': lands,
        'creature_hand_indices': creatures,
        'attack_table_indices': bears[:8],
    }
    print(json.dumps(plan, separators=(',', ':')), flush=True)

