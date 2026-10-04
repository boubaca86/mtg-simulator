from stage6_action_consistency_model import (
    CompleteAction,
    aggregate_complete_actions,
    strategy_fusion_upper_bound,
)


def test_same_complete_action_is_used_across_worlds():
    bolt_a = CompleteAction("Lightning Bolt", targets=("creature-A",))
    bolt_b = CompleteAction("Lightning Bolt", targets=("creature-B",))

    # Perfect information wants A in world 1 and B in world 2.  A real player
    # cannot condition the target on which unknown hand/library world is real.
    worlds = [
        {bolt_a: 100.0, bolt_b: 20.0},
        {bolt_a: 45.0, bolt_b: 90.0},
    ]
    winner, scores = aggregate_complete_actions(worlds, downside_weight=0.0)

    assert winner == bolt_a
    assert scores[bolt_a] == 72.5
    assert scores[bolt_b] == 55.0
    # This 95-point number is the impossible strategy-fused policy.  It must
    # never be compared as though it were a legal candidate action.
    assert strategy_fusion_upper_bound(worlds) == 95.0
    assert max(scores.values()) < strategy_fusion_upper_bound(worlds)


def test_modes_and_x_are_part_of_action_identity():
    small = CompleteAction("Modal X Spell", modes=("damage",), x=2, targets=("A",))
    large = CompleteAction("Modal X Spell", modes=("damage",), x=5, targets=("A",))
    worlds = [{small: 7.0, large: 3.0}, {small: 5.0, large: 9.0}]
    winner, _ = aggregate_complete_actions(worlds, downside_weight=0.0)
    assert winner == small


def test_action_missing_from_one_world_is_not_hidden_signal():
    safe = CompleteAction("Public Action", targets=("A",))
    leaky = CompleteAction("Action Whose Availability Differs", targets=("B",))
    worlds = [{safe: 5.0, leaky: 100.0}, {safe: 4.0}]
    winner, scores = aggregate_complete_actions(worlds)
    assert winner == safe
    assert leaky not in scores


def test_downside_penalty_is_explicit_and_reproducible():
    swingy = CompleteAction("Swingy")
    robust = CompleteAction("Robust")
    worlds = [{swingy: 100.0, robust: 61.0}, {swingy: 20.0, robust: 61.0}]
    winner, _ = aggregate_complete_actions(worlds, downside_weight=0.15)
    assert winner == robust
