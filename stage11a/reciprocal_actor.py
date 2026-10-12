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
# Stage11J: official vanilla {2}{G}{G} 4/4 extends visible size decisions.
VANILLA_FOURS = frozenset(('Rumbling Baloth',))
VANILLA_FIGHTERS = VANILLA_BEARS | VANILLA_THREES | VANILLA_FOURS
CREATURES = frozenset(('Llanowar Elves',)) | VANILLA_FIGHTERS

def public_power(name):
    if name in VANILLA_FOURS:
        return 4
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
    # Prefer greater known creature strength when actual mana permits it.
    # Forge remains solely responsible for paying {2}{G}{G} correctly.
    creatures.sort(key=lambda i: (-public_power(hand[i])
                                   if hand[i] in VANILLA_FIGHTERS else 0, i))
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
    # Stage11R: extend the emergency double-block decision to the
    # smaller *known vanilla 3/3* attacker as well as Stage11P/Q's
    # 4/4. Two distinct public 2/2 defenders at <=3 own life can
    # kill one 3/3 threat together; a single sacrificial 2/2 would
    # prevent this combat's damage but leave the 3/3 alive.
    # CR 510.1a/c/d and 510.2: simultaneous damage. The attacker
    # chooses how its 3 damage is divided between two legal blockers;
    # it can kill one 2/2 but not both. No readiness, priority or
    # actual attacking identity is inferred by either Python agent.
    # Only an exactly recognized public 3/3 or 4/4 enemy and
    # visible basic Forest lands may coexist; all unknown/facedown
    # permanents disable this narrow numerical decision.
    single_threat_name = (
        view['opponent_table'][targets[0]]['card']
        if len(targets) == 1 else None)
    single_threat_power = (
        public_power(single_threat_name)
        if single_threat_name in ('Centaur Courser', 'Rumbling Baloth')
        else 0)
    emergency_joint = (
        0 < life <= single_threat_power and
        len(fighters) == 2 and
        all(2 <= public_power(view['own_cards']['table'][i]['card'])
            < single_threat_power for i in fighters) and
        sum(public_power(view['own_cards']['table'][i]['card'])
            for i in fighters) >= single_threat_power and
        all(record.get('appearance') == 'face_up' and
            record.get('card') in (single_threat_name, 'Forest')
            for record in view['opponent_table']))
    if emergency_joint:
        reserve_count = 2
    reserved = fighters[-reserve_count:] if reserve_count else []
    # Stage11J: do not offer a weaker creature to an obviously stronger
    # public defender. A 4/4 can profitably attack into a 3/3; a 3/3
    # should not attack into a visible 4/4 absent other incentives.
    # True blocker readiness remains entirely private to the referee.
    public_largest_defender = max(
        (public_power(view['opponent_table'][i]['card']) for i in targets),
        default=0)
    candidate_attackers = fighters[:len(fighters) - reserve_count]
    active_attacks = [
        i for i in candidate_attackers
        if public_power(view['own_cards']['table'][i]['card'])
           >= public_largest_defender
    ][:8]
    # Stage11N: CR 508.1 / 509.1a. A vanilla defender can normally
    # block ONE attacker. Even if every own creature is smaller than
    # the largest visible blocker, a numerical attack can be lethal:
    # three 2/2s into one 4/4 leave at least two 2/2s unblocked.
    # Compute a conservative public-only damage floor: assume each
    # visible opposing creature blocks our highest-power attacker.
    # No tap/sickness/phase/hidden information is inferred. The
    # authoritative Forge referee alone selects legal attackers,
    # blocking choices, native damage, and winner.
    opposing_life = view['life_totals']['opponent']
    unblocked_strengths = sorted(
        (public_power(view['own_cards']['table'][i]['card'])
         for i in candidate_attackers), reverse=True)
    # Face-down and unrecognized visible permanents are not assumed
    # incapable of blocking. Count ALL public opposing table entries as
    # potential blockers; their secret identities are never requested.
    possible_blockers = len(view['opponent_table'])
    guaranteed_unblocked = sum(unblocked_strengths[possible_blockers:])
    if (0 < opposing_life <= guaranteed_unblocked
            and len(candidate_attackers) <= 8):
        active_attacks = candidate_attackers[:]
    # Stage11O: bounded NONLETHAL numerical overload. If a vanilla
    # 4/4 anchor already attacks into one 4/4, adding a 2/2 makes at
    # least 2 combat damage get through (assuming both legal attackers,
    # with one ordinary blocker). Keep ordinary defense reserves and
    # only press a nearby public opponent-life threshold. Every public
    # opposing permanent must be a KNOWN vanilla creature here; unknown
    # or face-down cards invalidate the tactical estimate. No actor
    # knows priority, phase, tap/sickness, a hidden hand or library.
    elif (len(candidate_attackers) <= 8
          and 0 < guaranteed_unblocked < opposing_life
          and opposing_life <= guaranteed_unblocked + 4
          and life > incoming + 4
          and len(candidate_attackers) > possible_blockers
          and public_largest_defender > 0
          and len(active_attacks) > 0
          and any(public_power(view['own_cards']['table'][i]['card'])
                  < public_largest_defender for i in candidate_attackers)
          and all(record.get('appearance') == 'face_up'
                  and record.get('card') in VANILLA_FIGHTERS
                  for record in view['opponent_table'])):
        active_attacks = candidate_attackers[:]
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
    # Stage11K: a generic coordinated block is profitable when TWO weaker
    # vanilla creatures jointly match the public threat's toughness.
    # Example: 3/3 + 2/2 against a 4/4, or 2/2 + 2/2 against a 3/3.
    # Prefer a solo equal/stronger blocker if one is visibly available.
    # These are only candidate indices, not actual combat assignments:
    # Forge knows real attacking identity, tapped state and legal timing.
    if life > incoming or emergency_joint:
        for enemy_i in ordered_targets:
            threat_power = public_power(view['opponent_table'][enemy_i]['card'])
            candidates = sorted(defensive_candidates,
                key=lambda i: -public_power(view['own_cards']['table'][i]['card']))
            if any(public_power(view['own_cards']['table'][i]['card'])
                   >= threat_power for i in candidates):
                continue
            if (len(candidates) >= 2 and
                sum(public_power(view['own_cards']['table'][i]['card'])
                    for i in candidates[:2]) >= threat_power):
                for own_i in candidates[:2]:
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

