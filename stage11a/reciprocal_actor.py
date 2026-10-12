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
# Stage11H: known public vanilla 3/3 for {2}{G}, unlike {1}{G} 2/2s.
VANILLA_THREES = frozenset(('Centaur Courser',))
VANILLA_FIGHTERS = VANILLA_BEARS | VANILLA_THREES
CREATURES = frozenset(('Llanowar Elves',)) | VANILLA_FIGHTERS

def public_power(name):
    return 3 if name in VANILLA_THREES else 2

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
    creatures.sort(key=lambda i: (hand[i] not in VANILLA_THREES,
                                  hand[i] not in VANILLA_FIGHTERS, i))
    fighters = [
        i for i, record in enumerate(view['own_cards']['table'])
        if record.get('appearance') == 'face_up'
        and record.get('card') in VANILLA_FIGHTERS
    ]
    # Stage11C: do not stop deploying creatures after the first Bear.
    # Additional Grizzly Bears in our OWN visible hand remain candidates
    # even if we already control Bears. Forge alone checks priority, costs,
    # mana, summoning sickness and actual spell legality. This is a
    # reinforcement preference, not a command to put any card in play.
    targets = [i for i, record in enumerate(view['opponent_table'])
               if record.get('appearance') == 'face_up'
               and record.get('card') in VANILLA_FIGHTERS]
    # Stage11D: when PUBLIC opposing 2/2 Bears could deal lethal damage,
    # hold back the minimum number of our 2/2 Bears which, IF each legally
    # blocks one attacker, could prevent it. Attacking would tap a Bear and
    # make it unavailable for ordinary blocks on the opponent's next turn
    # (CR 508.1f, 509.1a). All legality, timing, tap/sickness and actual
    # attacker selection remain Forge-private and referee-authoritative.
    # This is a conservative estimate: visible Bears may not attack, and
    # some of our own Bears might be unable to block. Do not pretend the
    # observation exposes readiness, hidden abilities, or combat state.
    incoming = sum(public_power(view['opponent_table'][i]['card'])
                   for i in targets)
    life = view['life_totals']['self']
    # Stage11E: prepare ONE untapped reserve a full 2/2 trade earlier.
    # When life is up to four ABOVE a visible single-swing threat, keeping
    # one Bear home can stop a follow-up attack and trade with an attacker.
    # This uses only public life and visible Bears, never hidden readiness,
    # draw order or referee phase; Forge remains the sole legal arbiter.
    if 0 < life <= incoming:
        reserve_count = min(len(fighters), (incoming - life) // 2 + 1)
    elif targets and 0 < life <= incoming + 4:
        reserve_count = min(len(fighters), 1)
    else:
        reserve_count = 0
    reserved = fighters[-reserve_count:] if reserve_count else []
    # 2/2 attacks risk an unfavourable block from a PUBLIC 3/3;
    # only Forge knows if that opposing creature can actually block.
    visible_three = any(view['opponent_table'][i]['card'] in VANILLA_THREES
                        for i in targets)
    active_attacks = [
        i for i in fighters[:len(fighters) - reserve_count]
        if not visible_three or
           view['own_cards']['table'][i]['card'] in VANILLA_THREES
    ][:8]
    # Stage11F: a healthy defender should still offer legal 2/2 blocks.
    # Attack and defense proposals coexist without assuming the phase.
    # A creature may have attacked already (and be tapped), so ONLY Forge
    # decides whether its offered block is legal in the current combat.
    # Candidate fallbacks let one untapped Bear try each publicly visible
    # enemy Bear; no actor receives the private actual-attacker list.
    # The trusted referee enforces distinct blockers and the eight-pair cap.
    defensive_candidates = reserved if reserve_count else fighters
    # Match strongest known defender to strongest public threat first.
    # At healthy life avoid sacrificing 2/2 to 3/3, but at lethal risk
    # offer that chump block as a fallback. Forge decides real legality.
    defensive_candidates = sorted(defensive_candidates,
        key=lambda i: -public_power(view['own_cards']['table'][i]['card']))
    ordered_targets = sorted(targets,
        key=lambda i: -public_power(view['opponent_table'][i]['card']))
    block_pairs = []
    # Stage11I: two independently controlled 2/2s may cooperatively block
    # one public 3/3. Combined 4 damage defeats it; a solo 2/2 cannot.
    # The same pair is offered against each visible 3/3 as a fallback,
    # because actual attacker identities remain referee-private.
    # CR 509.1a, 510.1a-d. Only Forge validates and resolves combat.
    smaller = [i for i in defensive_candidates
               if view['own_cards']['table'][i]['card'] in VANILLA_BEARS]
    stronger = [i for i in defensive_candidates
                if view['own_cards']['table'][i]['card'] in VANILLA_THREES]
    if life > incoming and not stronger and len(smaller) >= 2:
        for enemy_i in ordered_targets:
            if view['opponent_table'][enemy_i]['card'] not in VANILLA_THREES:
                continue
            for own_i in smaller[:2]:
                if len(block_pairs) < 8:
                    block_pairs.append([own_i, enemy_i])
    # Preserve the single-block candidate fallback plan. Never disclose
    # combat phase, actual attackers, tapped flags or private game objects.
    for own_i in defensive_candidates:
        own_strength = public_power(view['own_cards']['table'][own_i]['card'])
        for enemy_i in ordered_targets:
            enemy_strength = public_power(view['opponent_table'][enemy_i]['card'])
            if own_strength < enemy_strength and life > incoming:
                continue
            if len(block_pairs) >= 8:
                break
            pair = [own_i, enemy_i]
            if pair not in block_pairs:
                block_pairs.append(pair)
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

