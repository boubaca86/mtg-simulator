"""Stage 8Y fail-closed tests: face-down exile never becomes policy knowledge."""
import unittest
from patch_forge_stage8x_observed_only import patch_ai_controller

SOURCE = """    public boolean usesHybridSimulation() {
        return simMode == AIOption.USE_HYBRID_SIMULATION;
    }
    public boolean usesFullSimulation() {
        return simMode == AIOption.USE_FULL_SIMULATION;
    }
    private List<SpellAbility> getPossibleETBCounters() {
        CardCollection all = new CardCollection(player.getCardsIn(ZoneType.Hand));
        CardCollectionView ccvPlayerLibrary = player.getCardsIn(ZoneType.Library);

        all.addAll(player.getCardsIn(ZoneType.Exile));
        all.addAll(player.getCardsIn(ZoneType.Graveyard));
        if (!ccvPlayerLibrary.isEmpty()) {
            all.add(ccvPlayerLibrary.get(0));
        }

        all.addAll(player.getOpponents().getCardsIn(ZoneType.Exile));
    }
"""

class ExilePrivacyTests(unittest.TestCase):
    def test_both_exile_zones_are_visibility_filtered(self):
        patched=patch_ai_controller(SOURCE)
        self.assertEqual(patched.count("exiled.getView().canBeShownTo(player.getView())"),2)
        self.assertNotIn("all.addAll(player.getCardsIn(ZoneType.Exile));",patched)
        self.assertNotIn("all.addAll(player.getOpponents().getCardsIn(ZoneType.Exile));",patched)
    def test_graveyard_remains_public(self):
        self.assertIn("all.addAll(player.getCardsIn(ZoneType.Graveyard));",patch_ai_controller(SOURCE))
    def test_library_top_remains_visibility_filtered(self):
        self.assertIn("ccvPlayerLibrary.get(0).getView().canBeShownTo(player.getView())",patch_ai_controller(SOURCE))
    def test_old_simulator_still_disabled(self):
        s=patch_ai_controller(SOURCE)
        self.assertIn("!expertObservedOnly() && simMode == AIOption.USE_FULL_SIMULATION",s)
    def test_missing_opponent_exile_anchor_fails_closed(self):
        with self.assertRaises(ValueError):
            patch_ai_controller(SOURCE.replace("all.addAll(player.getOpponents().getCardsIn(ZoneType.Exile));",""))
    def test_missing_own_exile_anchor_fails_closed(self):
        with self.assertRaises(ValueError):
            patch_ai_controller(SOURCE.replace("all.addAll(player.getCardsIn(ZoneType.Exile));",""))
if __name__ == "__main__": unittest.main()
