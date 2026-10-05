"""Shared fail-closed validation for offline Stage 7 model inputs."""
import math

from stage7_label_outcomes import LABEL_VERSION

FORBIDDEN = {'opponent_hand', 'opponent_hand_cards', 'own_library',
             'opponent_library', 'library_order', 'future_draws'}
CARD_ZONES = ('own_hand', 'own_battlefield', 'opponent_battlefield',
              'own_graveyard', 'opponent_graveyard', 'battlefield_public',
              'graveyard_public', 'exile_public')
SEMANTIC_ZONES = ('own_hand_semantics', 'own_battlefield_semantics',
                  'opponent_battlefield_semantics', 'own_graveyard_semantics',
                  'opponent_graveyard_semantics', 'exile_public_semantics',
                  'stack_public_semantics')
CURRENT_SCHEMAS = {'stage7c-v3', 'stage7d-v1'}


def reject_hidden(value):
    if isinstance(value, dict):
        bad = FORBIDDEN & value.keys()
        if bad:
            raise ValueError(f'hidden-information leakage: {sorted(bad)}')
        for child in value.values():
            reject_hidden(child)
    elif isinstance(value, list):
        for child in value:
            reject_hidden(child)


def stack_items(row):
    value = row.get('stack_public', [])
    # The old Java extractor serialized MagicStack.toString(). It is not a list
    # of cards. Only its exact empty form is safe to migrate without recapture.
    if value == '[]==[]==[]':
        return []
    if not isinstance(value, list) or not all(isinstance(x, str) for x in value):
        raise ValueError('stack_public must be a list; legacy nonempty text requires recapture')
    return value


def validate_observation(row):
    reject_hidden(row)
    if not row.get('complete_action_identity'):
        raise ValueError('missing complete action identity')
    for zone in CARD_ZONES:
        if zone in row and (not isinstance(row[zone], list) or
                            not all(isinstance(x, str) for x in row[zone])):
            raise ValueError(f'{zone} must be a list of visible card names')
    for zone in SEMANTIC_ZONES:
        if zone in row and (not isinstance(row[zone], list) or
                            not all(isinstance(x, str) for x in row[zone])):
            raise ValueError(f'{zone} must be a list of public card descriptors')
    stack_items(row)
    # The current Java capture deliberately does not export known opponent-hand
    # identities. Do not accept an unverified list from an external producer.
    if row.get('opponent_known_cards'):
        raise ValueError('opponent_known_cards lacks verified public-knowledge provenance')
    for key in ('acting_life', 'opponent_life', 'turn', 'opponent_unknown_hand_count',
                'own_library_count', 'opponent_library_count'):
        if key in row and (isinstance(row[key], bool) or not isinstance(row[key], (float, int))
                           or not math.isfinite(row[key])):
            raise ValueError(f'{key} must be finite numeric data')
    for key in ('turn', 'opponent_unknown_hand_count', 'own_library_count', 'opponent_library_count'):
        if key in row and row[key] < 0:
            raise ValueError(f'{key} must be nonnegative')


def validate_labeled_rows(rows, current_schema=False):
    if not rows:
        raise ValueError('empty dataset')
    winners = {}
    for row in rows:
        validate_observation(row)
        if 'game_result' not in row:
            raise ValueError('missing game_result')
        if not row.get('game_group'):
            raise ValueError('missing game_group')
        if row.get('label_version') != LABEL_VERSION:
            raise ValueError('obsolete outcome labels: regenerate from Forge logs')
        if 'terminal_winner_player' not in row:
            raise ValueError('missing terminal winner provenance')
        actor, winner = row.get('acting_player'), row['terminal_winner_player']
        if type(actor) is not int or actor not in (0, 1):
            raise ValueError('invalid zero-based acting_player')
        if winner is not None and (type(winner) is not int or winner not in (0, 1)):
            raise ValueError('invalid zero-based terminal winner')
        expected = .5 if winner is None else float(actor == winner)
        if type(row['game_result']) not in (int, float) or row['game_result'] != expected:
            raise ValueError('game_result disagrees with terminal winner and acting player')
        group = str(row['game_group'])
        if group in winners and winners[group] != winner:
            raise ValueError('inconsistent terminal winner within a complete game')
        winners[group] = winner
        if current_schema:
            required = {'acting_player_name', 'schema_version', 'run_seed', 'decision_index',
                        'phase', 'acting_life', 'opponent_life', 'turn', 'own_hand',
                        'opponent_unknown_hand_count', 'own_library_count', 'opponent_library_count',
                        'own_battlefield', 'opponent_battlefield', 'own_graveyard',
                        'opponent_graveyard', 'exile_public', 'stack_public', 'matchup_id'}
            if required - row.keys() or row.get('schema_version') not in CURRENT_SCHEMAS:
                raise ValueError('paired comparison requires a freshly captured current Stage 7 observation')
            if row.get('schema_version') == 'stage7d-v1':
                missing_semantics = set(SEMANTIC_ZONES) - row.keys()
                if missing_semantics:
                    raise ValueError(f'stage7d-v1 missing public semantic zones: {sorted(missing_semantics)}')
            if not isinstance(row['stack_public'], list):
                raise ValueError('current stack_public must be a structured list')
            if row.get('infoset_sample_count') != 3 or not row['complete_action_identity'].startswith('recipe=v2|'):
                raise ValueError('paired comparison requires the corrected three-world action audit')
            if row.get('search_policy') != 'fixed-root-v1':
                raise ValueError('paired comparison requires fixed-root action aggregation; recapture the corpus')
    return rows
