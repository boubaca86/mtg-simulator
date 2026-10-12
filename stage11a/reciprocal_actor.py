#!/usr/bin/env python3
"""Stage11A: symmetric independent attack/block player without referee-private inputs."""
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
# Three DISTINCT vanilla 1G 2/2 card names, at most four copies each.
VANILLA_BEARS = frozenset(('Grizzly Bears', 'Runeclaw Bear', 'Balduvian Bears'))
# Stage11I: bounded printed-card knowledge, consulted only for own/revealed
# names. No card database, opponent deck inference or hidden state is loaded.
VANILLA_POWER = {name: 2 for name in VANILLA_BEARS}
VANILLA_POWER['Centaur Courser'] = 3
CREATURES = frozenset(('Llanowar Elves',)) | frozenset(VANILLA_POWER)

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
    # Prefer the larger body if affordable; Forge can try a cheaper fallback.
    creatures.sort(key=lambda i: (-VANILLA_POWER.get(hand[i], 1), i))
    combatants = [
        i for i, record in enumerate(view['own_cards']['table'])
        if record.get('appearance') == 'face_up'
        and record.get('card') in VANILLA_POWER
    ]
    # Stage11C: do not stop deploying creatures after the first Bear.
    # Additional Grizzly Bears in our OWN visible hand remain candidates
    # even if we already control Bears. Forge alone checks priority, costs,
    # mana, summoning sickness and actual spell legality. This is a
    # reinforcement preference, not a command to put any card in play.
    targets = [i for i, record in enumerate(view['opponent_table'])
               if record.get('appearance') == 'face_up'
               and record.get('card') in VANILLA_POWER]
    # Prefer stopping the largest visible printed threat. This remains a
    # printed-power estimate, not information about modifiers or readiness.
    targets.sort(key=lambda i: (-VANILLA_POWER[view['opponent_table'][i]['card']], i))
    threat_damage = [VANILLA_POWER[view['opponent_table'][i]['card']] for i in targets]
    # Stage11D/I: when visible opposing creatures could deal lethal damage,
    # hold back enough combatants which, IF each legally blocks one of the
    # largest threats, could prevent it. Attacking would tap a creature and
    # make it unavailable for ordinary blocks on the opponent's next turn
    # (CR 508.1f, 509.1a). All legality, timing, tap/sickness and actual
    # attacker selection remain Forge-private and referee-authoritative.
    # This is a conservative estimate: visible Bears may not attack, and
    # some of our own Bears might be unable to block. Do not pretend the
    # observation exposes readiness, hidden abilities, or combat state.
    incoming = sum(threat_damage)
    life = view['life_totals']['self']
    # Stage11E: prepare ONE untapped reserve a full 2/2 trade earlier.
    # When life is up to four ABOVE a visible single-swing threat, keeping
    # one Bear home can stop a follow-up attack and trade with an attacker.
    # This uses only public life and visible Bears, never hidden readiness,
    # draw order or referee phase; Forge remains the sole legal arbiter.
    if 0 < life <= incoming:
        reserve_count = 0
        remaining = incoming
        for damage in threat_damage[:len(combatants)]:
            reserve_count += 1
            remaining -= damage
            if remaining < life:
                break
    elif targets and 0 < life <= incoming + 4:
        reserve_count = min(len(combatants), 1)
    else:
        reserve_count = 0
    reserved = combatants[-reserve_count:] if reserve_count else []
    active_attacks = combatants[:len(combatants) - reserve_count][:8]
    # Stage11F: a healthy defender should still offer legal 2/2 blocks.
    # Attack and defense proposals coexist without assuming the phase.
    # A creature may have attacked already (and be tapped), so ONLY Forge
    # decides whether its offered block is legal in the current combat.
    # Candidate fallbacks let one untapped Bear try each publicly visible
    # enemy Bear; no actor receives the private actual-attacker list.
    # The trusted referee enforces distinct blockers and the eight-pair cap.
    defensive_candidates = reserved if reserve_count else combatants
    block_pairs = []
    # Stage11H: offer distinct first-choice targets before alternatives.
    # A row-major Cartesian product made Forge commit every blocker to the
    # first legal attacker, contradicting the reserve damage estimate above.
    # Rotate each later round to retain fallbacks without starving later
    # blockers of the eight-pair budget. Readiness stays referee-private.
    for offset in range(len(targets)):
        for position, own_i in enumerate(defensive_candidates):
            if len(block_pairs) >= 8:
                break
            enemy_i = targets[(position + offset) % len(targets)]
            block_pairs.append([own_i, enemy_i])
        if len(block_pairs) >= 8:
            break
    plan = {
        'intent': 'action_plan',
        'land_hand_indices': lands,
        'creature_hand_indices': creatures,
        'attack_table_indices': active_attacks,
        'block_pairs': block_pairs,
    }
    print(json.dumps(plan, separators=(',', ':')), flush=True)

