import base64
import copy
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from stage8k_return_boundary_intervention import (
    _is_targeted,
    _write_request,
    _choose_intervention,
    ARM_PREFIX,
    CONTROL_PREFIX,
    compare_pair,
    aggregate,
)
from stage8d_shadow_policy import CAPTURE_PREFIX
from stage8e_returned_action_audit import RETURN_PREFIX, PASS_PREFIX
from stage8f_acceptance_audit import ACCEPT_PREFIX
from stage8h_lifecycle_audit import TERMINAL_PREFIX
from test_stage8d_shadow_policy import capture, policy
from test_stage8e_returned_action_audit import returned, passed
from test_stage8h_lifecycle_audit import accepted, terminal


class Stage8KReturnBoundaryTests(unittest.TestCase):
    def test_targeted_identity_detection(self):
        self.assertFalse(_is_targeted(
            "recipe=v2|ability=Servitor|targets=<none>|choices=<none>"
        ))
        self.assertTrue(_is_targeted(
            "recipe=v2|ability=Lightning Strike|targets=[Servitor (117)]|choices=<none>"
        ))
        with self.assertRaises(ValueError):
            _is_targeted("recipe=v2|ability=Malformed|choices=<none>")

    def test_request_binds_expected_forge_and_learned_identities(self):
        forge = "recipe=v2|ability=Forge Choice|targets=<none>|choices=<none>"
        learned = "recipe=v2|ability=Learned Choice|targets=[Target (9)]|choices=<none>"
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "request.tsv"
            digest = _write_request(path, 7, forge, learned)
            raw = path.read_bytes()
            self.assertEqual(hashlib.sha256(raw).hexdigest(), digest)
            parts = raw.decode().strip().split("\t")
            self.assertEqual(len(parts), 3)
            self.assertEqual(parts[0], "7")
            self.assertEqual(
                base64.urlsafe_b64decode(parts[1]).decode(), forge
            )
            self.assertEqual(
                base64.urlsafe_b64decode(parts[2]).decode(), learned
            )

    def test_gate_requires_targeted_return_boundary_coverage(self):
        good = {
            "intervention_planned": True,
            "intervention_applied": True,
            "intervention_targeted": True,
            "lifecycle_anomalies": 0,
            "failed_dispatches": 0,
            "invalid_requests_accepted": 0,
            "pre_intervention_drift": 0,
            "forge_search_unchanged": True,
            "forge_phase_deferral_unchanged": True,
            "controlled_side_baseline_score": 0.0,
            "controlled_side_result_score": 1.0,
        }
        report = aggregate([dict(good) for _ in range(4)], 4, 1)
        self.assertTrue(report["safety_gate_passed"])
        self.assertEqual(report["totals"]["targeted_interventions_applied"], 4)
        self.assertFalse(report["promotion_allowed"])
        self.assertFalse(report["broader_learned_control_allowed"])

        untargeted = [dict(good, intervention_targeted=False) for _ in range(4)]
        self.assertFalse(aggregate(untargeted, 4, 1)["safety_gate_passed"])

    def test_gate_fails_if_forge_phase_decision_was_changed(self):
        good = {
            "intervention_planned": True,
            "intervention_applied": True,
            "intervention_targeted": True,
            "lifecycle_anomalies": 0,
            "failed_dispatches": 0,
            "invalid_requests_accepted": 0,
            "pre_intervention_drift": 0,
            "forge_search_unchanged": True,
            "forge_phase_deferral_unchanged": True,
            "controlled_side_baseline_score": 1.0,
            "controlled_side_result_score": 1.0,
        }
        rows = [dict(good) for _ in range(4)]
        rows[2]["forge_phase_deferral_unchanged"] = False
        self.assertFalse(aggregate(rows, 4, 1)["safety_gate_passed"])


class Stage8KAuditIntegrityTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        root = Path(self.temp.name)
        self.baseline, self.controlled = root / 'baseline.log', root / 'controlled.log'
        self.plan, self.request = root / 'plan.json', root / 'request.tsv'
        self.policy = policy()
        self.capture = capture()
        self.capture.update(decision_index=0, matchup_id='fixture')
        self.capture['selected_action'] = self.capture['candidates'][0]['action_identity']
        self.capture['public_state'].update(decision_index=0, acting_player=0,
                                           complete_action_identity=self.capture['selected_action'])
        self.baseline.write_text('\n'.join(self.lines()) + '\n')
        self.chosen = _choose_intervention(self.policy, self.baseline, 'Ai(1)-')
        self.controlled.write_text('\n'.join(self.lines(self.chosen)) + '\n')
        digest = _write_request(self.request, 0, self.chosen['forge_action'], self.chosen['learned_action'])
        self.metadata = dict(schema_version='stage8k-return-boundary-plan-v1',
            model_id=self.policy.model_id, actor_prefix='Ai(1)-', forge_referee=True,
            promotion_allowed=False, early_search_substitution_allowed=False,
            intervention_planned=True, intervention=self.chosen, request_file_sha256=digest)
        self.save_plan()

    def save_plan(self):
        self.plan.write_text(json.dumps(self.metadata))

    @staticmethod
    def encoded(prefix, event):
        return prefix + json.dumps(event)

    def lines(self, intervention=None):
        c = self.capture
        state = c['public_state']
        action = c['selected_action'] if intervention is None else intervention['learned_action']
        events = ['EXPERT_INFOSET_AUDITED_CANDIDATE: candidate=0 identifiedWorlds=3 samples=3']
        if intervention:
            events.append(self.encoded(ARM_PREFIX, dict(
                schema_version='stage8k-return-boundary-armed-v1', decision_index=0,
                acting_player_name=state['acting_player_name'], requested_action=action,
                forge_selected_action=c['selected_action'], requested_action_in_candidates=True,
                early_substitution=False, forge_referee=True, promotion_allowed=False)))
        events += [self.encoded('EXPERT_STAGE7_DATA: ', state), self.encoded(CAPTURE_PREFIX, c)]
        if intervention:
            events.append(self.encoded(CONTROL_PREFIX, dict(
                schema_version='stage8k-return-boundary-selection-v1', decision_index=0,
                acting_player_name=state['acting_player_name'], requested_action=action,
                forge_selected_action=c['selected_action'],
                substitution_boundary='post-forge-plan-pre-return', forge_search_unchanged=True,
                forge_phase_deferral_unchanged=True, forge_referee=True, promotion_allowed=False)))
        ret = returned(0, 0, action)
        ret['acting_player_name'] = state['acting_player_name']
        events += [self.encoded(RETURN_PREFIX, ret),
                   self.encoded(ACCEPT_PREFIX, accepted(idx=0, priority=0, action=action,
                       actor=state['acting_player_name'], turn=4)),
                   self.encoded(TERMINAL_PREFIX, terminal(idx=0, priority=0, action=action,
                       actor=state['acting_player_name'], return_turn=4, terminal_turn=4)),
                   'Game Result: Game 1 ended in 111 ms. Ai(1)-Example has won!']
        return events

    def change_event(self, prefix, **changes):
        lines = self.controlled.read_text().splitlines()
        for n, line in enumerate(lines):
            if line.startswith(prefix):
                event = json.loads(line[len(prefix):])
                event.update(changes)
                lines[n] = self.encoded(prefix, event)
        self.controlled.write_text('\n'.join(lines) + '\n')

    def compare(self):
        return compare_pair(self.policy, self.baseline, self.controlled,
                            self.plan, self.request, 'Ai(1)-')

    def test_valid_full_targeted_chain_passes_without_changing_forge_capture(self):
        result = self.compare()
        self.assertTrue(result['intervention_applied'])
        self.assertTrue(result['intervention_targeted'])
        self.assertEqual(result['failed_dispatches'], 0)
        self.assertTrue(aggregate([result], 1, 1)['safety_gate_passed'])

    def test_failed_dispatch_cannot_masquerade_as_resolved_action(self):
        self.change_event(ACCEPT_PREFIX, dispatch_success=False, skip_after_dispatch=True)
        with self.assertRaisesRegex(ValueError, 'disagrees with controller dispatch'):
            self.compare()

    def test_existing_acceptance_and_terminal_semantics_remain_enforced(self):
        for prefix, changes in (
            (ACCEPT_PREFIX, {'return_context_matches': False}),
            (ACCEPT_PREFIX, {'schema_version': 'unknown'}),
            (TERMINAL_PREFIX, {'fizzled': True}),
            (TERMINAL_PREFIX, {'stack_based': False}),
            (RETURN_PREFIX, {'promotion_allowed': True}),
        ):
            self.controlled.write_text('\n'.join(self.lines(self.chosen)) + '\n')
            self.change_event(prefix, **changes)
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                self.compare()

    def test_request_digest_and_exact_content_are_both_verified(self):
        original = self.request.read_bytes()
        self.request.write_bytes(original + b'99\tbad\tbad\n')
        with self.assertRaisesRegex(ValueError, 'request bytes or digest'):
            self.compare()
        # Updating the recorded digest cannot legalize a different request.
        self.metadata['request_file_sha256'] = hashlib.sha256(self.request.read_bytes()).hexdigest()
        self.save_plan()
        with self.assertRaisesRegex(ValueError, 'request bytes or digest'):
            self.compare()

    def test_plan_is_recomputed_from_frozen_model_and_first_eligible_decision(self):
        original = copy.deepcopy(self.metadata)
        for mutate in (
            lambda p: p.update(model_id='different-model'),
            lambda p: p['intervention'].update(learned_action=p['intervention']['forge_action']),
            lambda p: p['intervention'].update(learned_action_targeted=False),
            lambda p: p.update(intervention_planned=False, intervention=None),
        ):
            self.metadata = copy.deepcopy(original)
            mutate(self.metadata)
            self.save_plan()
            with self.subTest(metadata=self.metadata), self.assertRaises(ValueError):
                self.compare()

    def test_selection_after_return_or_terminal_after_game_is_rejected(self):
        original = self.lines(self.chosen)
        selection = next(l for l in original if l.startswith(CONTROL_PREFIX))
        terminal_line = next(l for l in original if l.startswith(TERMINAL_PREFIX))
        for moved in (selection, terminal_line):
            lines = [l for l in original if l != moved] + [moved]
            self.controlled.write_text('\n'.join(lines) + '\n')
            with self.subTest(moved=moved), self.assertRaises(ValueError):
                self.compare()

    def test_same_captures_do_not_hide_changed_phase_passes(self):
        lines = self.lines(self.chosen)
        for n, line in enumerate(lines):
            for prefix in (RETURN_PREFIX, ACCEPT_PREFIX, TERMINAL_PREFIX):
                if line.startswith(prefix):
                    event = json.loads(line[len(prefix):])
                    event['priority_return_index'] = 1
                    lines[n] = self.encoded(prefix, event)
        position = next(n for n, l in enumerate(lines) if l.startswith(CONTROL_PREFIX))
        extra_pass = passed(0)
        extra_pass['acting_player_name'] = self.capture['public_state']['acting_player_name']
        lines.insert(position, self.encoded(PASS_PREFIX, extra_pass))
        self.controlled.write_text('\n'.join(lines) + '\n')
        with self.assertRaisesRegex(ValueError, 'phase/pass events drifted'):
            self.compare()

    def test_later_index_phase_probe_is_part_of_pre_return_evidence(self):
        probe = copy.deepcopy(self.capture)
        probe['decision_index'] = probe['public_state']['decision_index'] = 1
        for path, prefix in ((self.baseline, RETURN_PREFIX), (self.controlled, CONTROL_PREFIX)):
            lines = path.read_text().splitlines()
            position = next(n for n, l in enumerate(lines) if l.startswith(prefix))
            if path == self.controlled:
                probe['public_state']['acting_life'] -= 1
            lines[position:position] = [self.encoded('EXPERT_STAGE7_DATA: ', probe['public_state']),
                                       self.encoded(CAPTURE_PREFIX, probe)]
            path.write_text('\n'.join(lines) + '\n')
        with self.assertRaisesRegex(ValueError, 'phase/pass events drifted'):
            self.compare()

    def test_gate_counts_failed_dispatches_even_without_anomaly_label(self):
        result = self.compare()
        result['failed_dispatches'] = 1
        report = aggregate([result], 1, 1)
        self.assertFalse(report['safety_gate_passed'])
        self.assertEqual(report['totals']['failed_dispatches'], 1)

    def test_no_intervention_requires_identical_play_and_result(self):
        self.policy = policy({})
        self.controlled.write_text(self.baseline.read_text())
        self.request.write_bytes(b'')
        self.metadata.update(intervention_planned=False, intervention=None,
                             request_file_sha256=hashlib.sha256(b'').hexdigest())
        self.save_plan()
        self.assertFalse(self.compare()['intervention_applied'])
        self.controlled.write_text(self.controlled.read_text().replace(
            'Ai(1)-Example has won!', 'Ai(2)-Opponent has won!'))
        with self.assertRaisesRegex(ValueError, 'no-intervention game changed'):
            self.compare()

    def test_controlled_candidates_retain_full_public_input_validation(self):
        for changes in ({'replay_valid_count': 2}, {'opponent_hand': ['secret']}):
            c = copy.deepcopy(self.capture)
            c['candidates'][0].update(changes)
            self.controlled.write_text('\n'.join(self.lines(self.chosen)) + '\n')
            self.change_event(CAPTURE_PREFIX, candidates=c['candidates'])
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                self.compare()


if __name__ == '__main__':
    unittest.main()
