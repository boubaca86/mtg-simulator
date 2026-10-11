"""Synthetic contract and separate-process checks, NOT real Forge gameplay."""
import json
import os
import subprocess
import sys
import tempfile
import unittest
from dataclasses import replace

from referee_life_protocol import RefereeMemory, RefereeSession, TableCard, validate_observation


class LifeRefereeTests(unittest.TestCase):
    def setUp(self):
        self.memory = RefereeMemory(
            objective="Win under the referee's Magic rules",
            hands={"a": ("Forest", "Shock"), "b": ("Island", "Counterspell")},
            libraries_in_draw_order={"a": ("Mountain", "Forest"), "b": ("Opt", "Island")},
            starting_decklists={"a": ("Shock", "Forest", "Forest", "Mountain"),
                                "b": ("Island", "Island", "Counterspell", "Opt")},
            table=(TableCard("a", "a", "Forest", "Forest"),
                   TableCard("b", "b", "Island", "Island"),
                   TableCard("b", "b", "SECRET", None, face_down=True),
                   TableCard("b", "b", "NOT_ON_TABLE", "NOT_ON_TABLE", played_on_table=False)),
            life_totals={"a": 20, "b": 20},
        )
        self.session = RefereeSession(self.memory)

    def read(self, actor="a", session=None):
        return json.loads((session or self.session).actor_observation(actor))

    def test_life_visible_and_symmetrical(self):
        a = self.read("a")
        b = self.read("b")
        self.assertEqual(a["life_totals"], {"self": 20, "opponent": 20})
        self.assertEqual(b["life_totals"], {"self": 20, "opponent": 20})
        self.assertEqual(set(a), {"objective", "own_cards", "opponent_table", "life_totals"})

    def test_referee_resolved_damage_heal_updates_both(self):
        self.session.referee_apply_resolved_life({"a": 17, "b": 23})
        self.assertEqual(self.read("a")["life_totals"], {"self": 17, "opponent": 23})
        self.assertEqual(self.read("b")["life_totals"], {"self": 23, "opponent": 17})
        self.session.referee_apply_resolved_life({"a": 5, "b": 0})
        self.assertEqual(self.read("a")["life_totals"], {"self": 5, "opponent": 0})

    def test_negative_life_allowed_but_not_internal_victory_oracle(self):
        self.session.referee_apply_resolved_life({"a": -2, "b": 7})
        a = self.read()
        self.assertEqual(a["life_totals"]["self"], -2)
        self.assertNotIn("game_over", a)
        self.assertNotIn("winner", a)

    def test_actor_cannot_set_or_propose_life(self):
        for cmd in (b'{"intent":"set_life","self":999}',
                    b'{"intent":"pass","life_totals":{"self":999}}',
                    b'{"intent":"damage","target":"b","amount":100}',
                    b'{"intent":"pass","intent":"set_life"}',
                    b'{"intent":"pass","intent":"pass"}',
                    b'null', b'[]', b'{"intent":false}',
                    b'{"intent":"pass","opponent_hand":[]}',
                    b'{"intent":"pass"' + b'x'*5000):
            with self.subTest(cmd=cmd[:50]), self.assertRaises(ValueError):
                self.session.actor_intent("a", cmd)
        self.assertEqual(self.read()["life_totals"]["self"], 20)
        self.assertEqual(self.session.actor_intent("a", b'{"intent":"pass"}'), b'{"accepted":true}')
        with self.assertRaises(ValueError):
            self.session.actor_intent("intruder", b'{"intent":"pass"}')

    def test_referee_updates_atomic_no_partial_or_bool(self):
        before = self.session.actor_observation("a")
        for broken in ({"a":10}, {"a":10,"b":11,"other":4},
                       {"a":True,"b":20}, {"a":"10","b":5}):
            with self.assertRaises(ValueError):
                self.session.referee_apply_resolved_life(broken)
        self.assertEqual(self.session.actor_observation("a"),before)

    def test_external_mutable_life_mapping_cannot_tamper(self):
        initial = {"a":20,"b":20}
        original = replace(self.memory, life_totals=initial)
        initial["a"] = 999
        self.assertEqual(json.loads(original.actor_observation("a"))["life_totals"]["self"],20)

    def test_noninterference_hidden_hand(self):
        changed = replace(self.memory, hands={"a": self.memory.hands["a"],"b": ("Something Else",)})
        self.assertEqual(self.memory.actor_observation("a"),changed.actor_observation("a"))
        self.assertNotEqual(self.memory.actor_observation("b"),changed.actor_observation("b"))

    def test_noninterference_own_or_opponent_library_order(self):
        for actor in ("a", "b"):
            changed = replace(self.memory,libraries_in_draw_order={
                **self.memory.libraries_in_draw_order,
                actor:tuple(reversed(self.memory.libraries_in_draw_order[actor]))})
            self.assertEqual(self.memory.actor_observation("a"),changed.actor_observation("a"))
            self.assertEqual(self.memory.actor_observation("b"),changed.actor_observation("b"))

    def test_noninterference_concealed_opponent_identity(self):
        cards = list(self.memory.table)
        cards[2] = replace(cards[2], private_name="CHANGED_SECRET")
        changed = replace(self.memory, table=tuple(cards))
        self.assertEqual(self.memory.actor_observation("a"), changed.actor_observation("a"))
        self.assertNotIn("CHANGED_SECRET", str(self.read()))

    def test_own_hand_and_visible_card_work(self):
        changed = replace(self.memory, hands={**self.memory.hands,"a":("Forest", "Giant Growth")})
        self.assertNotEqual(self.memory.actor_observation("a"),changed.actor_observation("a"))
        table = list(self.memory.table)
        table[1] = replace(table[1], visible_name="Mountain", private_name="Mountain")
        self.assertNotEqual(self.memory.actor_observation("a"),replace(self.memory,table=tuple(table)).actor_observation("a"))

    def test_face_down_own_without_look_permission(self):
        table = self.memory.table + (TableCard("a","a","OWN_SECRET",None,face_down=True),)
        a = json.loads(replace(self.memory,table=table).actor_observation("a"))
        self.assertEqual(a["own_cards"]["table"][-1], {"appearance":"face_down"})
        permitted = table[:-1] + (replace(table[-1], actor_may_look_original=frozenset({"a"})),)
        a2 = json.loads(replace(self.memory,table=permitted).actor_observation("a"))
        self.assertEqual(a2["own_cards"]["table"][-1]["card"],"OWN_SECRET")

    def test_life_schema_strict(self):
        a = self.read()
        for bad in ({"self":20}, {"self":20,"opponent":10,"secret":1},
                    {"self":20.0,"opponent":20}, {"self":True,"opponent":20}):
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                validate_observation({**a,"life_totals":bad})
        for k in ("opponent_hand", "draw_order", "turn", "priority", "stack", "graveyard", "logs"):
            with self.subTest(key=k), self.assertRaises(ValueError):
                validate_observation({**a,k:"unapproved"})

    def test_opponent_face_down_rejects_identity(self):
        data = self.read()
        data["opponent_table"][1]["card"] = "SECRET"
        with self.assertRaises(ValueError):
            validate_observation(data)

    def test_multiset_order_independent(self):
        changed = replace(self.memory, starting_decklists={**self.memory.starting_decklists,
                          "a": tuple(reversed(self.memory.starting_decklists["a"]))})
        self.assertEqual(self.memory.actor_observation("a"), changed.actor_observation("a"))

    def test_two_isolated_worker_processes_receive_correct_life_updates(self):
        worker = '''import sys,json,hashlib,os
for line in sys.stdin.buffer:
 obj=json.loads(line)
 assert set(obj)=={"objective","own_cards","opponent_table","life_totals"}
 print(json.dumps({"pid":os.getpid(),"life":obj["life_totals"],"digest":hashlib.sha256(line.strip()).hexdigest()}),flush=True)'''
        with tempfile.TemporaryDirectory() as da, tempfile.TemporaryDirectory() as db:
            def launch(folder):
                return subprocess.Popen([sys.executable,"-I","-S","-u","-c",worker],cwd=folder,
                        env={"PYTHONIOENCODING":"utf-8"},stdin=subprocess.PIPE,stdout=subprocess.PIPE,
                        stderr=subprocess.PIPE,close_fds=True)
            a,b=launch(da),launch(db)
            try:
                def observe(proc,actor):
                    proc.stdin.write(self.session.actor_observation(actor)+b'\n');proc.stdin.flush()
                    return json.loads(proc.stdout.readline())
                a0,b0=observe(a,"a"),observe(b,"b")
                self.assertNotEqual(a0["pid"],b0["pid"])
                self.assertEqual(a0["life"],{"self":20,"opponent":20})
                self.session.referee_apply_resolved_life({"a":18,"b":7})
                a1,b1=observe(a,"a"),observe(b,"b")
                self.assertEqual(a1["life"],{"self":18,"opponent":7})
                self.assertEqual(b1["life"],{"self":7,"opponent":18})
                self.assertNotEqual(a0["digest"],a1["digest"])
                self.assertNotEqual(b0["digest"],b1["digest"])
            finally:
                for p in (a,b):
                    p.stdin.close()
                    p.wait(timeout=10)
                    p.stdout.close()
                    p.stderr.close()
                self.assertEqual(a.returncode,0)
                self.assertEqual(b.returncode,0)


if __name__ == "__main__":
    unittest.main()
