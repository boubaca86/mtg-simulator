import json, tempfile, unittest
from pathlib import Path
import stage7_semantic_value_model as m

class Stage7CSemanticTests(unittest.TestCase):
    def row(self, group='g1'):
        return {'game_group':group,'game_result':1.0,'complete_action_identity':'cast:X','acting_life':10,'opponent_life':8,'own_hand':['Bolt'],'opponent_unknown_hand_count':2,'own_library_count':30,'opponent_library_count':31,'own_battlefield':['Goblin Guide'],'opponent_battlefield':['Bear'],'own_graveyard':['Bolt'],'opponent_graveyard':['Shock'],'exile_public':[],'stack_public':[],'turn':4}
    def write(self,rows):
        f=tempfile.NamedTemporaryFile('w',delete=False)
        with f:
            for r in rows: f.write(json.dumps(r)+'\n')
        return f.name
    def test_rejects_hidden_identity(self):
        r=self.row(); r['opponent_hand_cards']=['Secret']
        with self.assertRaisesRegex(ValueError,'hidden-information leakage'): m.load(self.write([r]))
    def test_zone_ownership_changes_features(self):
        a=self.row(); b=self.row(); b['own_battlefield'],b['opponent_battlefield']=b['opponent_battlefield'],b['own_battlefield']
        self.assertNotEqual(m.features(a,64),m.features(b,64))
    def test_deterministic_hash_features(self):
        r=self.row(); self.assertEqual(m.features(r,64),m.features(r,64))
    def test_unknown_counts_do_not_require_identities(self):
        r=self.row(); x=m.features(r,64); self.assertTrue(len(x)>64)

if __name__=='__main__': unittest.main()
