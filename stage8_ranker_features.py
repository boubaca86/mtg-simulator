"""Audited public context/action interactions. Never consume labels or game IDs.

Exact Forge recipes remain intact in the dataset and at execution. Only learning
features remove transient object IDs and candidate indexes. Ambiguous same-name
targets remain ambiguous; this module does not invent a public object mapping.
"""
from __future__ import annotations
import math
import re
from collections import Counter

MODELS = ('action_only', 'public_counts', 'visible_identity', 'public_semantics')
ZONES = ('own_hand', 'own_battlefield', 'opponent_battlefield', 'own_graveyard',
         'opponent_graveyard', 'exile_public', 'stack_public')
SCALES = {'turn': 20., 'acting_life': 20., 'opponent_life': 20.,
          'opponent_unknown_hand_count': 10., 'own_library_count': 60.,
          'opponent_library_count': 60.}


def number(value):
    if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(value):
        raise ValueError('public numeric feature must be finite and non-boolean')
    return float(value)


def canonical_action(identity, actor_name=''):
    """No winner/selected-action input. X, modes, choices and target names survive."""
    if not identity.startswith('recipe=v2|'):
        raise ValueError('ranker requires a captured recipe=v2 complete action')
    parts = {}
    seen = set()
    for piece in re.split(r'\|(?=(?:ability|candidate|x|modes|targets|choices)=)', identity):
        key, sep, value = piece.partition('=')
        if sep:
            if key in seen:
                raise ValueError('duplicate complete action field')
            seen.add(key)
        if sep and key in ('ability', 'x', 'modes', 'targets', 'choices'):
            if key == 'targets':
                if actor_name:
                    value = value.replace(actor_name, 'SELF')
                value = re.sub(r'Ai\(\d+\)-[^,\]]+', 'OPPONENT', value)
            if key in ('ability', 'targets', 'choices'):
                # Forge renders an object as "Card Name (123)". Preserve bare
                # numeric modes/choices and parenthesized costs such as "(2)".
                value = re.sub(r'(?<=\S)\s+\(\d+\)(?=[:,\];]|$)', '', value)
            parts[key] = ' '.join(value.lower().split())
    if not {'ability', 'candidate', 'x', 'modes', 'targets', 'choices'} <= seen:
        raise ValueError('ranker requires a captured recipe=v2 complete action')
    return '|'.join(f'{k}={parts.get(k, "<none>")}' for k in ('ability','x','modes','targets','choices'))


def descriptor(text):
    parts = text.split('|')
    attrs = dict(p.split('=', 1) for p in parts[1:] if '=' in p)
    out = {'name': parts[0].lower(), 'type': attrs.get('type', '').lower()}
    for key in ('mv', 'p', 't'):
        if key in attrs:
            try:
                out[key] = number(float(attrs[key]))
            except (ValueError, OverflowError) as exc:
                raise ValueError(f'invalid public descriptor {key}') from exc
    return out


def public_context(state, model):
    # Explicit allowlist. In particular, complete_action_identity, selected_action,
    # run_seed, decision_index, player/deck names and outcomes are NOT features.
    out = {'bias': 1., 'phase='+str(state.get('phase', '')): 1.}
    for key, scale in SCALES.items():
        out[key] = number(state.get(key, 0)) / scale
    out['life_advantage'] = out['acting_life'] - out['opponent_life']
    for zone in ZONES:
        cards = state.get(zone, [])
        if not isinstance(cards, list) or not all(isinstance(x, str) for x in cards):
            raise ValueError(f'{zone} must contain public card names')
        out[zone+':count'] = len(cards) / 10.
        if model in ('visible_identity', 'public_semantics'):
            for name, count in Counter(x.lower() for x in cards).items():
                out[zone+':name='+name] = count / 4.
        if model == 'public_semantics':
            descriptions = state.get(zone+'_semantics', [])
            if not isinstance(descriptions, list) or not all(isinstance(x, str) for x in descriptions):
                raise ValueError(f'{zone}_semantics must be public descriptors')
            for text in descriptions:
                d = descriptor(text)
                for key in ('mv', 'p', 't'):
                    out[zone+':'+key] = out.get(zone+':'+key, 0.) + d.get(key, 0.) / 10.
                for kind in ('land','creature','artifact','enchantment','instant','sorcery','planeswalker'):
                    if kind in d['type'].split():
                        out[zone+':type='+kind] = out.get(zone+':type='+kind, 0.) + .25
    return {k: v for k, v in out.items() if v}


def action_semantics(state, canonical):
    parts = dict(p.split('=', 1) for p in re.split(r'\|(?=(?:x|modes|targets|choices)=)', canonical))
    ability = parts['ability']
    out = {}
    for token in sorted(set(re.findall(r'[a-z]+', ability))):
        out['word='+token] = 1.
    for field in ('modes', 'x', 'choices'):
        if parts[field] != '<none>':
            out[field+'='+parts[field]] = 1.
    out['has_targets'] = float(parts['targets'] != '<none>')
    for role in ('self', 'opponent'):
        if re.search(r'\b'+role+r'\b', parts['targets']):
            out['target_role='+role] = 1.
    # Source characteristics come from currently visible hand/board descriptors.
    # Longest name match prevents a short card name swallowing a longer one.
    visible = [descriptor(x) for zone in ('own_hand', 'own_battlefield')
               for x in state.get(zone+'_semantics', [])]
    body = ability.split(': ', 1)[-1]
    matches = [d for d in visible if d['name'] and
               (ability.startswith(d['name']) or body.startswith(d['name']))]
    if matches:
        longest = max(len(d['name']) for d in matches)
        matches = [d for d in matches if len(d['name']) == longest]
        # Same-name objects can differ. Aggregate their public possibilities,
        # rather than pretending the recipe supplied an exact object mapping.
        for key in ('mv','p','t'):
            out['source_mean_'+key] = sum(d.get(key, 0.) for d in matches) / len(matches) / 10.
        for kind in ('land','creature','artifact','instant','sorcery'):
            out['source_type='+kind] = float(any(kind in d['type'].split() for d in matches))
    return {k: v for k, v in out.items() if v}


def features(row, candidate, model='public_semantics'):
    if model not in MODELS:
        raise ValueError(f'unknown ranker representation: {model}')
    state = row['public_state']
    action = canonical_action(candidate['action_identity'], state.get('acting_player_name', ''))
    action_features = {'recipe='+action: 1.}
    if model == 'public_semantics':
        action_features.update(action_semantics(state, action))
    if model == 'action_only':
        return {('action', k): v for k, v in action_features.items()}
    context = public_context(state, model)
    # The product is essential: adding f(state) + f(action) alone cancels the
    # entire state term in every within-decision comparison and training update.
    return {('interaction', a, s): av*sv
            for a, av in action_features.items() for s, sv in context.items()}
