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

LANDS = frozenset(('Forest', 'Island', 'Plains', 'Swamp', 'Mountain', 'Wastes', 'Jungle Hollow'))
# Three DISTINCT vanilla 1G 2/2 card names, at most four copies each.
VANILLA_BEARS = frozenset(('Grizzly Bears', 'Runeclaw Bear', 'Balduvian Bears'))
# Stage11H: known public vanilla 3/3 for {2}{G}, unlike {1}{G} 2/2s.
VANILLA_THREES = frozenset(('Centaur Courser',))
# Stage11J: official vanilla {2}{G}{G} 4/4 extends visible size decisions.
VANILLA_FOURS = frozenset(('Rumbling Baloth',))
VANILLA_FIGHTERS = VANILLA_BEARS | VANILLA_THREES | VANILLA_FOURS
# Stage12G: genuine printed {4}{G}{G} 6/6 Trample finisher.
TRAMPLE_FINISHERS = frozenset(('Colossal Dreadmaw',))
KNOWN_FIGHTERS = VANILLA_FIGHTERS | TRAMPLE_FINISHERS
DRAW_CREATURES = frozenset(('Elvish Visionary',))
FLYING_CREATURES = frozenset(('Scryb Sprites',))
# Stage12G: printed {R} 1/1 Haste may attack on its entry turn.
HASTE_CREATURES = frozenset(('Raging Goblin',))
# Printed Kird Ape is 1/1, but Forge's continuous Forest ability makes it 2/3.
FOREST_SYNERGY_CREATURES = frozenset(('Kird Ape',))
# Stage12G: printed {1}{U}{U} Phantom Warrior cannot be blocked.
UNBLOCKABLE_CREATURES = frozenset(('Phantom Warrior',))
DEATHTOUCH_CREATURES = frozenset(('Sedge Scorpion',))
# Stage12G: printed {1}{B} 2/1 Lifelink, native damage restores life.
LIFELINK_CREATURES = frozenset(('Child of Night',))
# Stage12G: genuine printed {1}{B} 1/1 death-trigger life swing.
DRAIN_CREATURES = frozenset(('Zulaport Cutthroat',))
# Stage12G: printed {1}{G} 1/3 Landfall life engine, only public-land decisions.
LANDFALL_CREATURES = frozenset(('Kazandu Nectarpot',))
# Stage12G: printed {2}{B} 2/2 ETB draw-one AND lose-one life.
# Do not offer this mandatory life-loss creature when our public life <=1.
RISK_DRAW_CREATURES = frozenset(('Phyrexian Rager',))
# Stage12G: printed {2}{G}{G} engine rewards later creature casts.
ENGINE_CREATURES = frozenset(('Beast Whisperer',))
RAMP_SORCERIES = frozenset(('Rampant Growth',))
SWARM_SORCERIES = frozenset(('Overrun',))
KNOWN_SORCERIES = RAMP_SORCERIES | SWARM_SORCERIES
CREATURES = (frozenset(('Llanowar Elves',)) | VANILLA_FIGHTERS |
             DRAW_CREATURES | FLYING_CREATURES | HASTE_CREATURES | FOREST_SYNERGY_CREATURES | UNBLOCKABLE_CREATURES | DEATHTOUCH_CREATURES |
             LIFELINK_CREATURES |
             TRAMPLE_FINISHERS |
             DRAIN_CREATURES | LANDFALL_CREATURES | RISK_DRAW_CREATURES |
             ENGINE_CREATURES)

def public_power(name):
    if name in TRAMPLE_FINISHERS:
        return 6
    if name in VANILLA_FOURS:
        return 4
    return 3 if name in VANILLA_THREES else 2

for raw in sys.stdin.buffer:
    if len(raw) > 16384:
        raise SystemExit('oversized observation')
    message = json.loads(raw, object_pairs_hook=_strict_object_pairs,
                         parse_constant=_reject_nonfinite_constant,
                         parse_float=_strict_json_float)
    envelope = isinstance(message, dict) and set(message) == {'request', 'observation'}
    request_kind = message['request'] if envelope else None
    if envelope and request_kind not in ('own_opening_hand', 'own_hand_order'):
        raise SystemExit('unapproved IPC action type')
    view = message['observation'] if envelope else message
    opening = request_kind == 'own_opening_hand'
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
    # Stage12A: only names and indices in OUR current hand.
    # Never disclose cleanup timing, phase, priority, maximum size, count,
    # opponent hidden cards, or any shuffled-library information.
    if request_kind == 'own_hand_order':
        if len(hand) > 128:
            raise SystemExit('oversized own-hand ranking')
        needed = max([2] + [6 if name in TRAMPLE_FINISHERS else 5 if name in SWARM_SORCERIES else
                             4 if name in VANILLA_FOURS else 3
                            for name in hand
                            if name in VANILLA_FOURS | VANILLA_THREES | SWARM_SORCERIES | TRAMPLE_FINISHERS | UNBLOCKABLE_CREATURES])
        lands_in_hand = sorted((i for i, name in enumerate(hand) if name in LANDS),
                               key=lambda i: (hand[i] != 'Forest', i))
        preferred = set(lands_in_hand[:needed])
        def discard_rank(i):
            name = hand[i]
            if name not in LANDS and name not in CREATURES | KNOWN_SORCERIES:
                return (0, 0, i)
            if name in LANDS and i not in preferred:
                return (1, 0, i)
            if name in CREATURES or name in KNOWN_SORCERIES:
                return (2, public_power(name) if name in KNOWN_FIGHTERS else 1, i)
            return (3, 0, i)
        ordering = sorted(range(len(hand)), key=discard_rank)
        print(json.dumps({'intent': 'hand_order', 'hand_priority': ordering},
                         separators=(',', ':')), flush=True)
        continue
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
            if name in CREATURES or name in KNOWN_SORCERIES:
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
    # Stage12G: printed Jungle Hollow enters tapped, gains one native life,
    # and later produces black or green mana. At critical public life prefer
    # the life-gain land, accepting the real tempo loss. At healthy life
    # prioritize untapped basics. This is only an own-hand land preference;
    # Forge alone authorizes land drops, ETB tapped, life triggers and mana.
    urgent_hollow = 0 < view['life_totals']['self'] <= 5
    lands.sort(key=lambda i: (
        0 if urgent_hollow and hand[i] == 'Jungle Hollow' else
        2 if hand[i] == 'Jungle Hollow' else 1,
        hand[i] != ('Swamp' if (DRAIN_CREATURES | RISK_DRAW_CREATURES | LIFELINK_CREATURES).intersection(hand) else 'Forest'),
        hand[i] != 'Forest', hand[i] != 'Island', i))
    creatures = [i for i, name in enumerate(hand)
                 if name in CREATURES and not
                 (name in RISK_DRAW_CREATURES and view['life_totals']['self'] <= 1)]
    # Stage12E: strategic basic-land acceleration from our own hand.
    # No library order, timing, stack or hidden game fields are provided.
    own_forests = sum(r.get('appearance') == 'face_up' and
                      r.get('card') == 'Forest'
                      for r in view['own_cards']['table'])
    # Stage12F: genuine printed Overrun costs FIVE mana and provides
    # team-wide +3/+3 and Trample. The passive actor sees only public
    # life, known creatures/Forest and own hand, never game phase, priority,
    # tapped flags, hidden cards or future draws. Native Forge rules decide.
    own_fighters = sum(r.get('appearance') == 'face_up' and
                       r.get('card') in KNOWN_FIGHTERS
                       for r in view['own_cards']['table'])
    public_safe = all(r.get('appearance') == 'face_up' and
                      r.get('card') in (KNOWN_FIGHTERS | LANDS)
                      for r in view['opponent_table'])
    swarm_window = (own_forests >= 5 and own_fighters >= 2 and
                    0 < view['life_totals']['opponent'] <= 10 and public_safe)
    sorceries = [i for i, name in enumerate(hand)
                 if (name in RAMP_SORCERIES and own_forests <= 4) or
                    (name in SWARM_SORCERIES and swarm_window)]
    sorceries.sort(key=lambda i: (hand[i] not in SWARM_SORCERIES, i))
    # Prefer greater known creature strength when actual mana permits it.
    # Forge remains solely responsible for paying {2}{G}{G} correctly.
    # Stage12B: a genuine {1}{G} Elvish Visionary replaces itself on
    # entering via a mandatory Forge-resolved draw. At <=3 own hand cards,
    # prefer it over another 2-mana creature to replenish resources.
    # This is only a candidate order; Forge alone checks legal casting,
    # pays mana, puts the triggered ability on the stack and draws.
    # Stage12D: when a visible 4/4 threatens lethal at own life <=4,
    # prioritize our printed one-mana 1/1 Deathtouch defender.
    # This is only a public-only CAST preference; the referee alone
    # verifies real mana, priority, sickness, blocking and combat damage.
    urgent_deathtouch = (0 < view['life_totals']['self'] <= 4 and
        any(r.get('appearance') == 'face_up' and
            r.get('card') == 'Rumbling Baloth'
            for r in view['opponent_table']))
    # Stage12G: a visible own Bear can trade in native combat while
    # Zulaport Cutthroat's real printed death trigger drains an opponent
    # and gains one life. This is only an own-hand cast preference, never
    # a private phase, mana, attacker or stack instruction to Forge.
    drain_window = (0 < view['life_totals']['self'] <= 5 and
        any(r.get('appearance') == 'face_up' and
            r.get('card') in KNOWN_FIGHTERS
            for r in view['own_cards']['table']) and
        any(r.get('appearance') == 'face_up' and
            r.get('card') in KNOWN_FIGHTERS
            for r in view['opponent_table']))
    # At low public life, deploy the printed Landfall life engine before
    # vanilla attackers. Only native Forge land entries trigger real life gain.
    urgent_landfall = 0 < view['life_totals']['self'] <= 6
    # Use only own visible Forests and own hand to set up a repeatable
    # creature-cast draw engine. Forge alone checks actual mana, timing,
    # stack ordering, draws and whether each spell resolves.
    engine_window = (own_forests >= 4 and any(
        name in CREATURES and name not in ENGINE_CREATURES for name in hand))
    # Stage12G: at low public life, prioritize a printed lifelink 2/1.
    # No opponent hand, readiness, phase, priority or hidden card is consulted.
    urgent_lifelink = 0 < view['life_totals']['self'] <= 5
    # Stage12G: an already-visible Beast Whisperer and a hand-held Elvish
    # Visionary create TWO independent native draw triggers from one paid
    # creature spell: Whisperer on cast, Visionary on battlefield entry.
    # Prefer Visionary ahead of ordinary Bears even with a large hand.
    # This is public/own-hand ordering only, not a prediction of draws.
    visionary_chain = (own_forests >= 2 and any(
        r.get('appearance') == 'face_up' and
        r.get('card') in ENGINE_CREATURES
        for r in view['own_cards']['table']))
    # Opponent public life at one and no visible creature blockers:
    # prefer printed Haste to threaten an immediate native Forge win.
    # No actor receives turn, sickness, priority or stack information.
    urgent_haste = (0 < view['life_totals']['opponent'] <= 1 and
        all(r.get('appearance') == 'face_up' and r.get('card') in LANDS
            for r in view['opponent_table']))
    # Kird Ape's Forest-dependent 2/3 is worth casting before a 2/2 Bear
    # when a public Forest and Mountain are already under our control and
    # the opponent has two life with no visible creature defenders.
    # Actor sees no mana availability, timing, stack or library order.
    urgent_ape = (own_forests >= 1 and
        0 < view['life_totals']['opponent'] <= 2 and
        any(r.get('appearance') == 'face_up' and r.get('card') == 'Mountain'
            for r in view['own_cards']['table']) and
        all(r.get('appearance') == 'face_up' and r.get('card') in LANDS
            for r in view['opponent_table']))
    creatures.sort(key=lambda i: (
        0 if urgent_ape and hand[i] in FOREST_SYNERGY_CREATURES else
        0 if urgent_haste and hand[i] in HASTE_CREATURES else
        0 if urgent_lifelink and hand[i] in LIFELINK_CREATURES else
        0 if own_forests >= 6 and hand[i] in TRAMPLE_FINISHERS else
        0 if visionary_chain and hand[i] in DRAW_CREATURES else
        0 if engine_window and hand[i] in ENGINE_CREATURES else
        0 if urgent_landfall and hand[i] in LANDFALL_CREATURES else
        0 if urgent_deathtouch and hand[i] in DEATHTOUCH_CREATURES else
        0 if drain_window and hand[i] in DRAIN_CREATURES else
        0 if len(hand) <= 3 and view['life_totals']['self'] > 1
             and hand[i] in RISK_DRAW_CREATURES else
        1 if len(hand) <= 3 and hand[i] in DRAW_CREATURES else 2,
        -public_power(hand[i]) if hand[i] in KNOWN_FIGHTERS else 0, i))
    fighters = [
        i for i, record in enumerate(view['own_cards']['table'])
        if record.get('appearance') == 'face_up'
        and record.get('card') in KNOWN_FIGHTERS
    ]
    # Stage12C: a real printed 1/1 Flying creature can evade even a
    # grounded visible 4/4. No readiness/phase/tap status is inferred.
    # Forge decides actual legal attacks and CR 702.9b blocking restrictions.
    flyers = [i for i, record in enumerate(view['own_cards']['table'])
              if record.get('appearance') == 'face_up'
              and record.get('card') in FLYING_CREATURES]
    # Stage11C: do not stop deploying creatures after the first Bear.
    # Additional Grizzly Bears in our OWN visible hand remain candidates
    # even if we already control Bears. Forge alone checks priority, costs,
    # mana, summoning sickness and actual spell legality. This is a
    # reinforcement preference, not a command to put any card in play.
    targets = [i for i, record in enumerate(view['opponent_table'])
               if record.get('appearance') == 'face_up'
               and record.get('card') in KNOWN_FIGHTERS]
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
    # Stage12C: the grounded 4/4 is NOT a legal blocker for Scryb Sprites.
    # Only grant this preference if every visible opponent permanent is
    # an identified printed grounded creature or ordinary basic land.
    # Unknown/facedown and any known flying creature disable the inference.
    known_grounded = (KNOWN_FIGHTERS | DRAW_CREATURES |
                      frozenset(('Llanowar Elves',)) | LANDS)
    if all(record.get('appearance') == 'face_up'
           and record.get('card') in known_grounded
           for record in view['opponent_table']):
        active_attacks.extend(flyers[:max(0, 8 - len(active_attacks))])
    # Stage12G: propose real Haste attackers even on their casting turn
    # if no publicly visible creature can block. Forge alone determines
    # summoning sickness, legal attackers, combat damage and game outcome.
    if all(r.get('appearance') == 'face_up' and r.get('card') in LANDS
           for r in view['opponent_table']):
        for i, r in enumerate(view['own_cards']['table']):
            if (len(active_attacks) < 8 and r.get('appearance') == 'face_up'
                    and r.get('card') in HASTE_CREATURES
                    and i not in active_attacks):
                active_attacks.append(i)
    # Offer a Forest-enhanced Kird Ape as a potential native 2/3 attacker.
    # The referee alone checks that it is untapped, not summoning-sick,
    # legally attacking, and has actual continuous-effect power/toughness.
    if own_forests >= 1 and all(
            r.get('appearance') == 'face_up' and r.get('card') in LANDS
            for r in view['opponent_table']):
        for i, r in enumerate(view['own_cards']['table']):
            if (len(active_attacks) < 8 and r.get('appearance') == 'face_up'
                    and r.get('card') in FOREST_SYNERGY_CREATURES
                    and i not in active_attacks):
                active_attacks.append(i)
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
    # Stage11S GAMEPLAY: In these strictly vanilla-only creature matches,
    # publicly identified ordinary basic lands cannot themselves block
    # (CR 509.1a). They therefore do NOT consume one of the opponent's
    # blocker slots. All unrecognized/facedown permanents still count
    # conservatively as possible blockers: never infer their identities.
    # Forge alone checks actual status, animation, evasion, legal blocks,
    # combat damage and winners; no private referee fields are exposed.
    possible_blockers = sum(
        1 for record in view['opponent_table']
        if record.get('appearance') != 'face_up'
        or record.get('card') not in LANDS)
    guaranteed_unblocked = sum(unblocked_strengths[possible_blockers:])
    # Stage11T: an IMMEDIATELY lethal public vanilla attack may justify
    # committing the final creature normally reserved for next-turn defense.
    # Example at own life 4, opposing life 4: three known 2/2s against
    # one known 4/4 leave two unblocked creatures dealing four real damage
    # IF Forge permits all three legal attackers. Reserving one 2/2
    # instead would forfeit that immediate-win opportunity.
    # Bound the calculation to eight visible own vanilla creatures,
    # and require every opposing permanent to be a known vanilla
    # fighter or ordinary basic land: unknown/facedown cards INVALIDATE
    # the exclusive blocker calculation. Never infer actual readiness,
    # opposing hand, priority, timing, stack or future draws.
    # This is only an ATTACK PREFERENCE; Forge remains sole authority.
    full_lethal_strengths = sorted(
        (public_power(view['own_cards']['table'][i]['card'])
         for i in fighters), reverse=True)
    full_unblocked = sum(full_lethal_strengths[possible_blockers:])
    public_lethal_reserve_override = (
        reserve_count > 0 and len(fighters) <= 8 and
        0 < opposing_life <= full_unblocked and
        opposing_life > guaranteed_unblocked and
        all(record.get('appearance') == 'face_up' and
            record.get('card') in (KNOWN_FIGHTERS | LANDS)
            for record in view['opponent_table']))
    if public_lethal_reserve_override:
        active_attacks = fighters[:]
    elif (0 < opposing_life <= guaranteed_unblocked
            and len(candidate_attackers) <= 8):
        active_attacks = candidate_attackers[:]
    # Stage11U GAMEPLAY: a whole squad of vanilla creatures individually
    # weaker than the public defender may still apply useful NONLETHAL
    # pressure. Three visible 2/2s against one known 4/4 can send at least
    # four player damage through the one-creature/one-blocker rule
    # (CR 509.1a, 510.1b), even though each 2/2 alone is a poor trade.
    # Distinct from Stage11N's immediate lethal swarm and Stage11O's mixed
    # 4/4+2/2 pressure, this offers an all-underpowered three-creature
    # attack at opposing life 6-8, only while our own life is safely above
    # visible incoming strength. Never count unknown/facedown permanents
    # as known vanilla blockers, never claim legal readiness or outcomes.
    # Actual legality, combat losses and player damage belong to Forge.
    elif (len(candidate_attackers) == 3
          and all(public_power(view['own_cards']['table'][i]['card'])
                  == 2 for i in candidate_attackers)
          and public_largest_defender == 4
          and possible_blockers == 1
          and 6 <= opposing_life <= 8
          and guaranteed_unblocked == 4
          and life > incoming + 4
          and all(record.get('appearance') == 'face_up'
                  and record.get('card') in (KNOWN_FIGHTERS | LANDS)
                  for record in view['opponent_table'])):
        active_attacks = candidate_attackers[:]
    # Stage11O/11S: bounded NONLETHAL numerical overload. A vanilla
    # 4/4 plus 2/2 pressure one vanilla 4/4 blocker; a known Forest
    # is not another creature. Keep reserves and use only nearby public
    # opponent life. Every public permanent must be known vanilla or
    # basic land; unknown/facedown records disable this estimate.
    # No actor knows priority, phase, tap/sickness, hidden cards/order.
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
                  and record.get('card') in (KNOWN_FIGHTERS | LANDS)
                  for record in view['opponent_table'])):
        active_attacks = candidate_attackers[:]
    # Printed Phantom Warrior can attack past all blockers. Forge alone
    # checks legal attacks, summoning sickness, taps, damage and outcome.
    for i, r in enumerate(view['own_cards']['table']):
        if (len(active_attacks) < 8 and r.get('appearance') == 'face_up'
                and r.get('card') in UNBLOCKABLE_CREATURES
                and i not in active_attacks):
            active_attacks.append(i)
    # Stage12G: an identified printed Lifelink attacker can replenish
    # public life while applying pressure when only public basic lands
    # oppose it. This is an ATTACK PREFERENCE, never a claim of readiness
    # or guaranteed damage: native Forge decides legality and life gain.
    if all(r.get('appearance') == 'face_up' and r.get('card') in LANDS
           for r in view['opponent_table']):
        for i, r in enumerate(view['own_cards']['table']):
            if (len(active_attacks) < 8 and r.get('appearance') == 'face_up'
                    and r.get('card') in LIFELINK_CREATURES
                    and i not in active_attacks):
                active_attacks.append(i)
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
    # Stage11V: at <=4 public life, block TWO visible 4/4 attackers
    # separately with our 4/4 (trade) and 2/2 (chump). The old greedy
    # strongest-first proposal double-blocked one and leaked lethal four.
    # This is a public-only priority; Forge checks real attackers, tapped
    # status, blocking legality, native damage and winner (CR 509-510).
    two_lane_emergency = (
        0 < life <= 4 and len(defensive_candidates) == 2
        and sorted(public_power(view['own_cards']['table'][i]['card'])
                   for i in defensive_candidates) == [2, 4]
        and len(targets) == 2
        and all(public_power(view['opponent_table'][i]['card']) == 4
                for i in targets)
        and all(record.get('appearance') == 'face_up'
                and record.get('card') in (KNOWN_FIGHTERS | LANDS)
                for record in view['opponent_table']))
    # Stage11W: an otherwise idle OWN face-up Llanowar Elves is a
    # legitimate 1/1 creature blocker. When exactly one known vanilla
    # opposing 2/2 could cause immediate loss at own life 1 or 2, offer
    # this fragile mana creature as an emergency chump block rather
    # than taking lethal player damage. This is NOT an attack proposal,
    # nor a tap/phase/readiness claim: Forge alone approves the legal
    # physical block, combat damage, creature death and player survival.
    # No inferencing about unknown or facedown opposing permanents.
    elves = [i for i, record in enumerate(view['own_cards']['table'])
             if record.get('appearance') == 'face_up'
             and record.get('card') == 'Llanowar Elves']
    emergency_elf_chump = (
        0 < life <= 2 and not fighters
        and len(elves) == 1 and len(targets) == 1
        and view['opponent_table'][targets[0]]['card'] in VANILLA_BEARS
        and all(record.get('appearance') == 'face_up'
                and record.get('card') in (VANILLA_BEARS | LANDS | {'Llanowar Elves'})
                for record in view['opponent_table']))
    if emergency_elf_chump:
        block_pairs.append([elves[0], targets[0]])
    # Stage11X: two separate publicly known vanilla 2/2 attackers at own
    # life <=2, with exactly one visible own 2/2 and one visible 1/1 Elf.
    # Trade the 2/2 on one lane and chump the other with the Elf.
    # These are ordered PREFERENCES, not declared blockers: the trusted
    # Forge referee checks real attackers, tap state, evasion, legal blocks,
    # simultaneous damage, deaths, and life. Unknown/facedown opposing
    # permanents disable this narrow public-only inference.
    two_lane_bear_elf = (
        0 < life <= 2 and len(fighters) == 1 and len(elves) == 1
        and view['own_cards']['table'][fighters[0]]['card'] in VANILLA_BEARS
        and len(targets) == 2
        and all(view['opponent_table'][i]['card'] in VANILLA_BEARS
                for i in targets)
        and all(record.get('appearance') == 'face_up'
                and record.get('card') in (VANILLA_BEARS | LANDS | {'Llanowar Elves'})
                for record in view['opponent_table']))
    if two_lane_bear_elf:
        block_pairs.extend([[fighters[0], ordered_targets[0]],
                            [elves[0], ordered_targets[1]]])
    # Stage11Y: when two distinct known vanilla 2/2 creatures could
    # deliver lethal at own life 1 or 2, spend TWO real registered
    # Llanowar Elves as separate emergency chump blockers. Preferences
    # only: Forge alone checks which creatures actually attacked, tap
    # state, legal blocks, combat damage, deaths and player survival.
    # Unknown and face-down public permanents disable this narrow plan.
    two_lane_double_elf = (
        0 < life <= 2 and not fighters and len(elves) == 2
        and len(targets) == 2
        and all(view['opponent_table'][i]['card'] in VANILLA_BEARS
                for i in targets)
        and all(record.get('appearance') == 'face_up'
                and record.get('card') in (VANILLA_BEARS | LANDS | {'Llanowar Elves'})
                for record in view['opponent_table']))
    if two_lane_double_elf:
        block_pairs.extend([[elves[0], ordered_targets[0]],
                            [elves[1], ordered_targets[1]]])
    # Stage11Z: TWO 1/1 Elves can kill ONE known attacking vanilla 2/2
    # by simultaneously dealing 1+1 combat damage. A single Elf would
    # only chump. This is a public-only emergency BLOCK PREFERENCE.
    # Forge alone validates legal untapped blockers, damage and deaths.
    # Do not infer any unknown or face-down opposing permanent's identity.
    emergency_double_elf_trade = (
        0 < life <= 2 and not fighters and len(elves) == 2
        and len(targets) == 1
        and view['opponent_table'][targets[0]]['card'] in VANILLA_BEARS
        and all(record.get('appearance') == 'face_up'
                and record.get('card') in (VANILLA_BEARS | LANDS | {'Llanowar Elves'})
                for record in view['opponent_table']))
    if emergency_double_elf_trade:
        block_pairs.extend([[elves[0], targets[0]],
                            [elves[1], targets[0]]])
    # Stage12D: one genuine Sedge Scorpion 1/1 with printed Deathtouch
    # can trade with a single visible 4/4 when its attack threatens our
    # <=4 life. Do NOT mistake this for a vanilla 1/1 chump block:
    # CR 702.2b destroys the 4/4 after one damage. No hidden cards,
    # readiness, actual combat or phase are supplied to the actor.
    scorpions = [i for i, record in enumerate(view['own_cards']['table'])
                 if record.get('appearance') == 'face_up'
                 and record.get('card') in DEATHTOUCH_CREATURES]
    emergency_deathtouch = (
        0 < life <= 4 and len(scorpions) == 1 and len(targets) == 1
        and view['opponent_table'][targets[0]]['card'] == 'Rumbling Baloth'
        and all(record.get('appearance') == 'face_up'
                and record.get('card') in (KNOWN_FIGHTERS | LANDS)
                for record in view['opponent_table']))
    if emergency_deathtouch:
        block_pairs.append([scorpions[0],targets[0]])
    # Stage12G: at critical public life, offer a genuine registered
    # Cutthroat as a sacrificial blocker against one vanilla 2/2. Its
    # death triggers a real one-life opponent drain and one-life gain,
    # resolved solely by Forge. The actor cannot see attacker status,
    # priority, stack, private cards, or any future library order.
    cutthroats = [i for i, record in enumerate(view['own_cards']['table'])
                  if record.get('appearance') == 'face_up'
                  and record.get('card') in DRAIN_CREATURES]
    emergency_drain_chump = (
        0 < life <= 2 and not fighters and len(cutthroats) == 1
        and len(targets) == 1
        and view['opponent_table'][targets[0]]['card'] in VANILLA_BEARS
        and all(record.get('appearance') == 'face_up'
                and record.get('card') in (VANILLA_BEARS | LANDS)
                for record in view['opponent_table']))
    if emergency_drain_chump:
        block_pairs.append([cutthroats[0], targets[0]])
    # Stage12G: a 2/3 Beast Whisperer can defend against a known 2/2,
    # kill that attacker, and survive at critical public life. Forge alone
    # decides actual attackers, readiness, legality and combat damage.
    whisperers = [i for i, r in enumerate(view['own_cards']['table'])
                  if r.get('appearance') == 'face_up'
                  and r.get('card') in ENGINE_CREATURES]
    engine_shield = (0 < life <= 2 and len(whisperers) == 1
        and not fighters and not elves and not scorpions and not cutthroats
        and len(targets) == 1
        and view['opponent_table'][targets[0]]['card'] in VANILLA_BEARS
        and all(r.get('appearance') == 'face_up'
                and r.get('card') in (VANILLA_BEARS | LANDS)
                for r in view['opponent_table'])
        and all(r.get('appearance') == 'face_up'
                and r.get('card') in (ENGINE_CREATURES | LANDS)
                for r in view['own_cards']['table']))
    if engine_shield:
        block_pairs.append([whisperers[0], targets[0]])
    if two_lane_emergency:
        strong = next(i for i in defensive_candidates
                      if public_power(view['own_cards']['table'][i]['card']) == 4)
        weak = next(i for i in defensive_candidates
                    if public_power(view['own_cards']['table'][i]['card']) == 2)
        block_pairs.extend([[strong, ordered_targets[0]],
                            [weak, ordered_targets[1]]])
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
        'sorcery_hand_indices': sorceries,
        'attack_table_indices': active_attacks,
        'block_pairs': block_pairs,
    }
    print(json.dumps(plan, separators=(',', ':')), flush=True)

