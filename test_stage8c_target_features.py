"""Stage 8C regressions for public target semantics."""
import unittest

from stage8_ranker_features import ZONES, features as stage8b_features
from stage8c_target_features import features as stage8c_features, parse_target_descriptor
from stage8c_target_ranker import compare


def recipe(index, object_id):
    return (f"recipe=v2|ability=Remove target creature|candidate={index}/2|x=<none>|modes=<none>"
            f"|targets=[Goblin ({object_id})]|choices=<none>")


def row(seed=101, decision=0):
    weak = recipe(0, 12)
    strong = recipe(1, 99)
    state = {
        "acting_life": 20, "opponent_life": 20, "turn": 4, "phase": "MAIN1",
        "acting_player_name": "Ai(1)-Example", "decision_index": decision,
        "complete_action_identity": strong,
        "opponent_unknown_hand_count": 3, "own_library_count": 40, "opponent_library_count": 40,
    }
    for zone in ZONES:
        state[zone] = []
        state[zone + "_semantics"] = []
    state["opponent_battlefield"] = ["Goblin", "Goblin"]
    state["opponent_battlefield_semantics"] = [
        "Goblin|mv=1|type=Creature|p=1|t=1",
        "Goblin|mv=1|type=Creature|p=5|t=5",
    ]
    return {
        "schema_version": "stage8a-v1", "forge_version": "2.0.15",
        "search_policy": "fixed-root-v1", "information_set_samples": 3,
        "decision_id": f"{seed}:{decision}", "game_group": f"{seed}:game",
        "corpus_seed": seed, "public_state": state, "selected_action": strong,
        "candidates": [
            {
                "action_identity": weak, "aggregate_score": 10, "replay_valid_count": 3,
                "target_public_semantics": [
                    "zone=opponent_battlefield|role=opponent|Goblin|mv=1|type=Creature|p=1|t=1"
                ],
            },
            {
                "action_identity": strong, "aggregate_score": 20, "replay_valid_count": 3,
                "target_public_semantics": [
                    "zone=opponent_battlefield|role=opponent|Goblin|mv=1|type=Creature|p=5|t=5"
                ],
            },
        ],
    }


class TargetFeatureTests(unittest.TestCase):
    def test_same_name_targets_gain_public_instance_semantics(self):
        r = row()
        a, b = r["candidates"]
        # Stage 8B intentionally strips transient object IDs, so these alias.
        self.assertEqual(stage8b_features(r, a, "public_semantics"),
                         stage8b_features(r, b, "public_semantics"))
        # Stage 8C distinguishes them using only descriptors Forge resolved while
        # each target was public; the object IDs themselves never cross the boundary.
        self.assertNotEqual(stage8c_features(r, a), stage8c_features(r, b))
        rendered = repr(stage8c_features(r, a)) + repr(stage8c_features(r, b))
        self.assertNotIn("12", rendered)
        self.assertNotIn("99", rendered)

    def test_raw_object_ids_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "object IDs"):
            parse_target_descriptor("zone=opponent_battlefield|role=opponent|Goblin|id=12|mv=1|type=Creature")

    def test_stage8c_requires_fresh_target_capture(self):
        r = row()
        del r["candidates"][0]["target_public_semantics"]
        with self.assertRaisesRegex(ValueError, "requires target_public_semantics"):
            stage8c_features(r, r["candidates"][0])

    def test_player_only_target_may_have_no_card_descriptor(self):
        r = row()
        c = r["candidates"][0]
        c["target_public_semantics"] = []
        result = stage8c_features(r, c)
        self.assertTrue(result)

    def test_target_aware_ranker_learns_when_stage8b_cannot(self):
        rows = [row(seed, decision) for seed in (101, 202, 303) for decision in range(3)]
        result = compare(rows)
        old = result["models"]["public_semantics"]["holdout"]
        new = result["models"]["target_semantics"]["holdout"]
        self.assertEqual(old["top1_accuracy"], 0.5)
        self.assertGreater(new["top1_accuracy"], old["top1_accuracy"])
        self.assertLess(new["normalized_regret"], old["normalized_regret"])
        self.assertFalse(result["promotion_allowed"])


if __name__ == "__main__":
    unittest.main()
