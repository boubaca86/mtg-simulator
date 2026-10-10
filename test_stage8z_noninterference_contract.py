"""Adversarial metamorphic checks for observation-only decision boundary.

Synthetic interface tests only; do NOT claim actual Forge game noninterference.
"""
import unittest
from stage8z_noninterference_contract import visible_decision

OWN = ["Mountain"] * 56 + ["Lightning Bolt"] * 4
PUBLIC = {
    "own_hand": ["Lightning Bolt"], "own_library_count": 53,
    "opponent_unknown_hand_count": 5, "opponent_library_count": 53,
    "opponent_battlefield": ["Island"], "stack_public": [],
}
ACTIONS = [
    {"id": "pass", "kind": "pass", "public_cost": "", "public_target": "", "public_effect": ""},
    {"id": "bolt-player", "kind": "cast", "public_cost": "R", "public_target": "opponent", "public_effect": "3 damage"},
]
def choose(**kwargs):
    return visible_decision(own_decklist=OWN, public=PUBLIC,
        observed_opponent_cards=["Island"], legal_visible_actions=ACTIONS,
        policy_seed=123, **kwargs)

class NoninterferenceTests(unittest.TestCase):
    def test_adversarial_hidden_hands_have_no_input_channel(self):
        baseline=choose()
        for actual_secret in (["Counterspell"]*5, ["Giant Growth"]*5,
                              ["Black Lotus"]*5, ["Island"]*5):
            # A referee may hold these different secrets; none is passed to policy.
            self.assertEqual(choose(), baseline)
            self.assertEqual(len(actual_secret), PUBLIC["opponent_unknown_hand_count"])
    def test_opponent_secret_metadata_rejected(self):
        with self.assertRaises(ValueError):
            visible_decision(own_decklist=OWN,
                public={**PUBLIC, "opponent_actual_hidden_hand": ["Counterspell"]},
                observed_opponent_cards=["Island"], legal_visible_actions=ACTIONS)
    def test_global_catalog_rejected(self):
        with self.assertRaises(ValueError):
            visible_decision(own_decklist=OWN,
                public={**PUBLIC, "global_card_catalog": ["Black Lotus"]},
                observed_opponent_cards=["Island"], legal_visible_actions=ACTIONS)
    def test_hidden_action_annotation_rejected(self):
        altered=[dict(a) for a in ACTIONS]
        altered[0]["opponent_secret"] = "Counterspell"
        with self.assertRaises(ValueError):
            visible_decision(own_decklist=OWN, public=PUBLIC,
                observed_opponent_cards=["Island"], legal_visible_actions=altered)
    def test_action_order_does_not_change_choice(self):
        baseline=choose()
        alternate=visible_decision(own_decklist=OWN, public=PUBLIC,
            observed_opponent_cards=["Island"], legal_visible_actions=list(reversed(ACTIONS)),
            policy_seed=123)
        self.assertEqual(baseline, alternate)
    def test_own_deck_order_does_not_change_choice(self):
        baseline=choose()
        alternate=visible_decision(own_decklist=list(reversed(OWN)), public=PUBLIC,
            observed_opponent_cards=["Island"], legal_visible_actions=ACTIONS,
            policy_seed=123)
        self.assertEqual(baseline, alternate)
    def test_duplicate_actions_fail_closed(self):
        with self.assertRaises(ValueError):
            visible_decision(own_decklist=OWN, public=PUBLIC,
                observed_opponent_cards=["Island"], legal_visible_actions=ACTIONS+ACTIONS[:1])
    def test_untrusted_seed_fails_closed(self):
        with self.assertRaises(ValueError):
            visible_decision(own_decklist=OWN, public=PUBLIC,
                observed_opponent_cards=["Island"], legal_visible_actions=ACTIONS, policy_seed="hidden")
    def test_no_legal_actions_fails_closed(self):
        with self.assertRaises(ValueError):
            visible_decision(own_decklist=OWN, public=PUBLIC,
                observed_opponent_cards=["Island"], legal_visible_actions=[])
    def test_observed_history_is_explicit_input(self):
        a=choose()
        b=visible_decision(own_decklist=OWN, public=PUBLIC,
            observed_opponent_cards=["Island", "Counterspell"], legal_visible_actions=ACTIONS,
            policy_seed=123)
        self.assertNotEqual(a["observation_fingerprint"], b["observation_fingerprint"])
if __name__ == "__main__":
    unittest.main()
