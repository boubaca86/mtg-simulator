import unittest
from pathlib import Path
from tempfile import TemporaryDirectory
from patch_forge_stage8x_observed_only import patch_ai_controller, patch_game_simulator, patch_sources
from stage8x_information_view import information_view, fingerprint

AI_SOURCE = """        all.addAll(player.getCardsIn(ZoneType.Exile));
        all.addAll(player.getCardsIn(ZoneType.Graveyard));
        if (!ccvPlayerLibrary.isEmpty()) {
            all.add(ccvPlayerLibrary.get(0));
        }
        all.addAll(player.getOpponents().getCardsIn(ZoneType.Exile));
    public boolean usesHybridSimulation() {
        return simMode == AIOption.USE_HYBRID_SIMULATION;
    }
    public boolean usesFullSimulation() {
        return simMode == AIOption.USE_FULL_SIMULATION;
    }
"""
SIM_SOURCE = """    public GameSimulator(SimulationController controller, Game origGame, Player origAiPlayer, PhaseType advanceToPhase) {
        this.controller = controller;
"""

def public():
    return {"own_hand":["Lightning Bolt"], "own_library_count":53,
            "opponent_unknown_hand_count":5,"opponent_library_count":53,
            "opponent_battlefield":["Island"], "stack_public":[]}

class ObservedOnlyTests(unittest.TestCase):
    def test_full_and_hybrid_fallback_default(self):
        s = patch_ai_controller(AI_SOURCE)
        self.assertIn("return !expertObservedOnly() && simMode == AIOption.USE_FULL_SIMULATION",s)
        self.assertIn("return !expertObservedOnly() && simMode == AIOption.USE_HYBRID_SIMULATION",s)
        self.assertIn('"true"', s)

    def test_own_top_card_only_if_visible(self):
        updated = patch_ai_controller(AI_SOURCE)
        self.assertIn("canBeShownTo(player.getView())",updated)

    def test_guard_before_copy(self):
        s = patch_game_simulator(SIM_SOURCE)
        self.assertLess(s.index("throw new IllegalStateException"),s.index("this.controller = controller"))

    def test_unexpected_forge_version_rejected(self):
        with self.assertRaises(ValueError): patch_ai_controller("wrong version")
        with self.assertRaises(ValueError): patch_game_simulator("wrong version")

    def test_no_partial_write_on_error(self):
        with TemporaryDirectory() as d:
            a,b=Path(d)/"AiController.java",Path(d)/"GameSimulator.java"
            a.write_text(AI_SOURCE);b.write_text("wrong version")
            with self.assertRaises(ValueError): patch_sources(a,b)
            self.assertEqual(a.read_text(), AI_SOURCE)

    def test_hidden_opponent_changes_cannot_reach_information_view(self):
        # Distinct secret decks must yield equal player-facing information.
        v1=information_view(own_decklist=["Mountain"]*56+["Lightning Bolt"]*4,
                            public=public(),observed_opponent_cards=["Island"])
        v2=information_view(own_decklist=["Lightning Bolt"]*4+["Mountain"]*56,
                            public=public(),observed_opponent_cards=["Island"])
        self.assertEqual(fingerprint(v1),fingerprint(v2))
        for hidden in (["Counterspell"],["Giant Growth"]):
            with self.assertRaises(ValueError):
                information_view(own_decklist=["Mountain"]*60,
                    public={**public(),"opponent_hidden_hand":hidden},
                    observed_opponent_cards=["Island"])

    def test_global_database_not_input(self):
        with self.assertRaises(ValueError):
            information_view(own_decklist=["Mountain"]*60,
                public={**public(),"magic_card_database":["Black Lotus"]},
                observed_opponent_cards=[])

    def test_seen_opponent_card_does_not_claim_current_location(self):
        view=information_view(own_decklist=["Mountain"]*60,
            public=public(),observed_opponent_cards=["Counterspell"])
        self.assertEqual(view["observed_opponent_card_names"],["Counterspell"])
        self.assertEqual(view["opponent_unknown_hand_slots"],5)
        self.assertNotIn("opponent_hand",view)

    def test_library_order_never_exposed(self):
        view=information_view(own_decklist=["Island","Mountain"],
            public=public(),observed_opponent_cards=[])
        self.assertTrue(view["own_unknown_library_order"])
        self.assertEqual(view["own_decklist_counts"],{"Island":1,"Mountain":1})

    def test_hidden_counts_must_be_nonnegative(self):
        snap=public();snap["opponent_library_count"]=-1
        with self.assertRaises(ValueError):
            information_view(own_decklist=["Island"],public=snap,observed_opponent_cards=[])

if __name__=="__main__": unittest.main()
