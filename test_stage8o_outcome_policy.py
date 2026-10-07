"""Stage 8O fail-closed public-only outcome-learning tests."""
import copy
import json
import tempfile
import unittest
from pathlib import Path

import stage8o_outcome_policy as o
from test_stage8l_outcome_trajectories import row as fixture
from stage8n_outcome_trajectories import _convert_row


def make_rows():
    early, later = [], []
    for collection, seeds, prefix, per_game, extra in (
        (early, (20261028, 20261029, 20261030, 20261031), "stage8l", 14, 3),
        (later, tuple(range(20261034, 20261042)), "stage8n", 13, 8),
    ):
        games_done = 0
        for seed in seeds:
            for side in ("st-first", "red-first"):
                game = f"{prefix}-controlled-{side}-{seed}.log"
                count = per_game + int(games_done < extra)
                for index in range(count):
                    entry = fixture(seed, learned=(index % 3 == 0))
                    entry["trajectory_id"] = f"{game}:decision-{index}"
                    entry["source_log"] = game
                    entry["trajectory_step"] = index
                    entry["audit"]["decision_index"] = index
                    entry["audit"]["run_seed"] = seed + index
                    entry["audit"]["corpus_seed"] = seed
                    entry["model_input"]["public_state"]["turn"] = 1+index//3
                    entry["model_input"]["public_state"]["acting_life"] = 15+(index%6)
                    entry["model_input"]["public_state"]["opponent_life"] = 20-(index%5)
                    entry["labels"]["game_result"] = float((seed+games_done)%2 == 0)
                    if prefix == "stage8n":
                        entry = _convert_row(entry)
                    collection.append(entry)
                games_done += 1
    return early,later


def write_rows(path,rows):
    path.write_text("".join(o.canonical(row)+"\n" for row in rows))


class Stage8OTests(unittest.TestCase):
    def test_complete_grouped_training_and_reserved_seeds(self):
        early,later=make_rows()
        checked=o.checked_rows(early,later)
        self.assertEqual(len(checked),331)
        self.assertEqual(len({(kind,row["source_log"]) for kind,row in checked}),24)
        self.assertTrue(set(o.TRAIN_SEEDS).isdisjoint(o.RESERVED_SEEDS))
        changed=copy.deepcopy(later)
        changed[0]["audit"]["corpus_seed"]=o.RESERVED_SEEDS[0]
        with self.assertRaises(ValueError):
            o.checked_rows(early,changed)

    def test_hidden_and_outcome_leakage_fails_closed(self):
        early,later=make_rows()
        changed=copy.deepcopy(early)
        changed[0]["model_input"]["public_state"]["opponent_hand"]=["secret"]
        with self.assertRaises(ValueError):
            o.checked_rows(changed,later)
        changed=copy.deepcopy(later)
        changed[0]["model_input"]["public_state"]["game_result"]=1.0
        with self.assertRaises(ValueError):
            o.checked_rows(early,changed)

    def test_model_cannot_invent_illegal_actions(self):
        early,_=make_rows()
        inp=early[0]["model_input"]
        a,b=inp["candidates"]
        x=o.features(inp,a)
        y=o.features(inp,b)
        self.assertNotEqual(x,y)
        self.assertTrue(all(isinstance(key,str) for key in x))
        self.assertNotIn("game_result",json.dumps(x))
        with self.assertRaises(ValueError):
            o.features(inp,{"action_identity":"invented"})

    def test_training_ignores_audit_and_label_fields_during_prediction(self):
        early,_=make_rows()
        inp=copy.deepcopy(early[0]["model_input"])
        action=inp["candidates"][0]
        predicted=o.features(inp,action)
        self.assertEqual(predicted,o.features(inp,action))
        extra=copy.deepcopy(inp)
        extra["public_state"]["run_seed"]=20261032
        with self.assertRaises(ValueError):
            o.features(extra,extra["candidates"][0])

    def test_grouped_cv_is_diagnostic_not_strength_proof(self):
        early,later=make_rows()
        cv=o.cross_validation(o.checked_rows(early,later))
        self.assertEqual(len(cv["folds"]),12)
        self.assertFalse(cv["strength_claim_allowed"])
        for fold in cv["folds"]:
            self.assertEqual(fold["held_games"],2)
            self.assertEqual(fold["train_games"],22)
        self.assertEqual(cv["development_gate_passed"],
            cv["model"]["logloss"] < cv["constant_baseline"]["logloss"]-1e-6
            and cv["model"]["brier"] < cv["constant_baseline"]["brier"]-1e-6)

    def test_checkpoint_is_reproducible_and_tampering_is_rejected(self):
        early,later=make_rows()
        with tempfile.TemporaryDirectory() as td:
            a,b=Path(td)/"early.jsonl",Path(td)/"later.jsonl"
            model=Path(td)/"model.json"
            write_rows(a,early)
            write_rows(b,later)
            first,report=o.export(a,b)
            self.assertEqual(first["training_rows"],331)
            self.assertEqual(report["cross_validation"]["method"],o.CONFIG["validation"])
            self.assertFalse(first["promotion_allowed"])
            self.assertFalse(first["broader_learned_control_allowed"])
            self.assertFalse(report["reserved_evaluation_opened"])
            model.write_text(o.canonical(first)+"\n")
            self.assertEqual(o.load(model)["model_id"],first["model_id"])
            first["weights"]["bias"]+=1
            model.write_text(o.canonical(first)+"\n")
            with self.assertRaisesRegex(ValueError,"hash"):
                o.load(model)


if __name__=="__main__":
    unittest.main()
