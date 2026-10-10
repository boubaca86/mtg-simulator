"""Development-only synthetic regressions: no reserved seeds or Forge games."""
import copy
import unittest

from test_stage8c_target_features import row
from stage8d_shadow_policy import public_input
from stage8q_target_effect_features import VERSION, features

ACTOR = "Ai(1)-Example"
OPPONENT = "Ai(2)-Opponent"


def candidate(ability, target, descriptor, number=0):
    identity = (f"recipe=v2|ability={ability}|candidate={number}/1|x=<none>|"
                f"modes=<none>|targets=[{target}]|choices=<none>")
    return {"action_identity": identity,
            "target_semantics_version": "forge-public-targets-v2",
            "target_public_semantics": [descriptor]}


def situation(options):
    r = row()
    r["public_state"]["acting_player_name"] = ACTOR
    r["candidates"] = options
    return public_input(r["public_state"], r["candidates"])


class Stage8QTests(unittest.TestCase):
    def test_burn_distinguishes_opponent_face_and_own_creature(self):
        ability = "Some Burn Spell deals 3 damage to any target."
        enemy = candidate(ability, OPPONENT, "zone=player|role=opponent|<player>")
        own = candidate(ability, "Friendly Goblin (21)",
                        "zone=own_battlefield|role=self|Friendly Goblin|mv=1|type=Creature - Goblin|p=1|t=1", 1)
        inp = situation([enemy, own])
        a, b = features(inp, enemy), features(inp, own)
        self.assertEqual(a["q_damage_to_opponent_player"], 0.25)
        self.assertEqual(a["q_damage_to_self_creature"], 0.0)
        self.assertEqual(b["q_damage_to_self_creature"], 0.25)
        self.assertEqual(b["q_self_creature_damage_vs_toughness"], 1.0)
        self.assertEqual(a["q_fixed_damage_amount"], .3)
        self.assertNotEqual(a, b)

    def test_opponent_creature_removal_is_not_own_creature_damage(self):
        burn = candidate("Spell deals 2 damage to target creature.", "Enemy Goblin (6)",
                         "zone=opponent_battlefield|role=opponent|Enemy Goblin|mv=1|type=Creature|p=2|t=2")
        f = features(situation([burn]), burn)
        self.assertEqual(f["q_damage_to_opponent_creature"], .25)
        self.assertEqual(f["q_opponent_creature_damage_vs_toughness"], 1.0)
        self.assertEqual(f["q_damage_to_self_creature"], 0.0)

    def test_beneficial_self_target_is_not_banned_or_labeled_as_damage(self):
        buff = candidate("Spell: Put a +1/+1 counter on target creature.", "My Ally (17)",
                         "zone=own_battlefield|role=self|My Ally|mv=2|type=Creature|p=2|t=2")
        f = features(situation([buff]), buff)
        self.assertEqual(f["q_pump_to_self_creature"], .25)
        self.assertEqual(f["q_damage_to_self_creature"], 0)
        self.assertEqual(f["q_fixed_damage_amount"], 0)

    def test_same_semantics_ignores_transient_object_ids_and_card_names(self):
        a = candidate("Red Bolt deals 3 damage to target creature.", "Goblin (1)",
                      "zone=own_battlefield|role=self|Goblin|mv=1|type=Creature|p=1|t=1")
        b = candidate("Blue Bolt deals 3 damage to target creature.", "Goblin (500)",
                      "zone=own_battlefield|role=self|Goblin|mv=1|type=Creature|p=1|t=1")
        # The fixed Stage 8O word features and the new interaction representation
        # must not use transient object IDs or card-name tokens.
        self.assertEqual(features(situation([a]), a), features(situation([b]), b))

    def test_hidden_information_is_rejected(self):
        a = candidate("Spell deals 3 damage to any target.", OPPONENT,
                      "zone=player|role=opponent|<player>")
        original = situation([a])
        for name, value in (("opponent_hand", ["Secret Card"]),
                            ("game_result", 1), ("future_draw", "Secret")):
            changed = copy.deepcopy(original)
            changed["public_state"][name] = value
            with self.subTest(name=name), self.assertRaises(ValueError):
                features(changed, changed["candidates"][0])

    def test_untrusted_candidate_and_invalid_target_descriptors_are_rejected(self):
        a = candidate("Spell deals 3 damage to any target.", OPPONENT,
                      "zone=player|role=opponent|<player>")
        inp = situation([a])
        forged = candidate("Spell deals 4 damage to any target.", OPPONENT,
                           "zone=player|role=opponent|<player>")
        with self.assertRaises(ValueError):
            features(inp, forged)
        bad = copy.deepcopy(inp)
        bad["candidates"][0]["target_public_semantics"] = [
            "zone=own_battlefield|role=opponent|Goblin|mv=1|type=Creature"]
        with self.assertRaisesRegex(ValueError, "inconsistent public target ownership"):
            features(bad, bad["candidates"][0])
        bad = copy.deepcopy(inp)
        bad["candidates"][0]["target_public_semantics"] = [
            "zone=player|role=opponent|<player>|id=900"]
        with self.assertRaises(ValueError):
            features(bad, bad["candidates"][0])

    def test_player_target_identity_cannot_disagree_with_recipe(self):
        a = candidate("Spell deals 3 damage to any target.", OPPONENT,
                      "zone=player|role=self|<player>")
        with self.assertRaisesRegex(ValueError, "disagrees with player action identity"):
            features(situation([a]), a)

    def test_schema_is_fixed_across_target_types_and_actions(self):
        burn = candidate("Spell deals 3 damage to any target.", OPPONENT,
                         "zone=player|role=opponent|<player>")
        untargeted = {"action_identity": ("recipe=v2|ability=Cast vanilla creature|candidate=0/1|"
                         "x=<none>|modes=<none>|targets=<none>|choices=<none>"),
                      "target_semantics_version":"forge-public-targets-v2", "target_public_semantics":[]}
        self.assertEqual(set(features(situation([burn]), burn)),
                         set(features(situation([untargeted]), untargeted)))
        self.assertEqual(VERSION, "stage8q-public-target-effect-v1")


if __name__ == "__main__":
    unittest.main()
