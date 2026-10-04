import json
import pytest

from stage7_legal_feature_contract import (
    DecisionObservation, KnownCard, export_legal_features, grouped_split_key,
)


def observation(**changes):
    base = dict(
        run_seed=101, decision_index=7, acting_player=1, on_play=True,
        turn=4, phase="MAIN1", step="", acting_life=17, opponent_life=13,
        own_hand=("Lightning Bolt",),
        opponent_known_cards=(KnownCard("Counterspell", True),),
        opponent_unknown_hand_count=3, own_library_count=47,
        opponent_library_count=45,
        battlefield_public=("Mountain", "Island"),
        graveyard_public=(), exile_public=(), stack_public=(),
        mana_public={"R": 1}, matchup_id="fixture-v1",
        complete_action_identity="Bolt|target=opponent", infoset_sample_count=3,
    )
    base.update(changes)
    return DecisionObservation(**base)


def test_deterministic_same_observation_same_serialization():
    a = export_legal_features(observation())
    b = export_legal_features(observation())
    assert json.dumps(a, sort_keys=True) == json.dumps(b, sort_keys=True)


def test_unknown_opponent_identity_is_rejected():
    with pytest.raises(ValueError, match="not public knowledge"):
        export_legal_features(observation(
            opponent_known_cards=(KnownCard("Secret Doom Blade", False),)
        ))


def test_schema_contains_counts_not_future_library_identities():
    row = export_legal_features(observation())
    keys = set(row)
    assert "opponent_library_count" in keys
    assert "own_library_count" in keys
    assert not any("library_cards" in k or "library_order" in k for k in keys)
    assert not any("unknown_hand_cards" in k for k in keys)


def test_complete_action_and_infoset_context_are_mandatory():
    with pytest.raises(ValueError):
        export_legal_features(observation(complete_action_identity=""))
    with pytest.raises(ValueError):
        export_legal_features(observation(infoset_sample_count=0))


def test_split_is_grouped_by_run_seed_not_decision():
    first = export_legal_features(observation(decision_index=1))
    later = export_legal_features(observation(decision_index=999))
    other = export_legal_features(observation(run_seed=102))
    assert grouped_split_key(first) == grouped_split_key(later)
    assert grouped_split_key(first) != grouped_split_key(other)
