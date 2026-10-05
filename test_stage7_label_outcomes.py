import json
import tempfile
import unittest
from pathlib import Path

from stage7_label_outcomes import LABEL_VERSION, label_log


class OutcomeLabelsTest(unittest.TestCase):
    def row(self, actor):
        return {"acting_player": actor, "acting_player_name": f"Ai({actor + 1})-Same Deck",
                "infoset_sample_count": 3, "complete_action_identity": "cast:test"}

    def game(self, winner=1, number=1):
        lines = ["EXPERT_INFOSET_AUDITED_CANDIDATE: candidate=0 identifiedWorlds=3 samples=3"]
        lines += ["EXPERT_STAGE7_DATA: " + json.dumps(self.row(a)) for a in (0, 1)]
        terminal = "ended in a Draw! Took 20 ms." if winner is None else f"ended in 20 ms. Ai({winner})-Same Deck has won!"
        return "\n".join(lines + [f"Game Result: Game {number} {terminal}"]) + "\n"

    def read(self, text, expected=1):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "corpus.log"
            p.write_text(text)
            return label_log(p, expected_games=expected)

    def test_first_display_seat_wins(self):
        rows = self.read(self.game(1))
        self.assertEqual([r["game_result"] for r in rows], [1., 0.])
        self.assertEqual({r["label_version"] for r in rows}, {LABEL_VERSION})

    def test_second_display_seat_wins(self):
        self.assertEqual([r["game_result"] for r in self.read(self.game(2))], [0., 1.])

    def test_draw_and_whole_game_identity(self):
        rows = self.read(self.game(None))
        self.assertEqual([r["game_result"] for r in rows], [.5, .5])
        self.assertEqual(rows[0]["game_group"], rows[1]["game_group"])

    def test_sequential_games_do_not_mix(self):
        rows = self.read(self.game(1) + self.game(2, 2), expected=2)
        self.assertEqual([r["game_result"] for r in rows], [1., 0., 0., 1.])
        self.assertEqual(len({r["game_group"] for r in rows}), 2)

    def test_timeout_cannot_become_a_win(self):
        with self.assertRaisesRegex(ValueError, "timeout"):
            self.read("Stopping slow match as draw\n" + self.game())

    def test_fusion_and_incomplete_audit_fail(self):
        for text in ("EXPERT_INFOSET_STRATEGY_FUSION: candidate=0 samples=3\n" + self.game(),
                     self.game().replace("identifiedWorlds=3", "identifiedWorlds=1"),
                     "\n".join(self.game().splitlines()[1:])):
            with self.assertRaises(ValueError):
                self.read(text)

    def test_missing_terminal_or_wrong_count_fail(self):
        for text, count in ((self.game().split("Game Result:")[0], 1), (self.game(), 2), (self.game(number=2), 1)):
            with self.assertRaises(ValueError):
                self.read(text, count)

    def test_invalid_actor_winner_and_name_fail(self):
        for text in (self.game().replace('"acting_player": 0', '"acting_player": 2'),
                     self.game(3), self.game().replace('"Ai(1)-Same Deck"', '"Ai(2)-Same Deck"')):
            with self.assertRaises(ValueError):
                self.read(text)


if __name__ == "__main__":
    unittest.main()
