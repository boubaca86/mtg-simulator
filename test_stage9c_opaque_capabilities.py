"""Synthetic Stage 9C referee/policy boundary tests; NOT live Forge tests."""
from __future__ import annotations

import unittest

from stage8z_noninterference_contract import visible_decision
from stage9c_opaque_capabilities import BlindExilePileReferee

_PUBLIC = {
    "own_hand": ["Lightning Bolt"],
    "own_library_count": 51,
    "opponent_unknown_hand_count": 4,
    "opponent_library_count": 52,
    "opponent_battlefield": ["Island"],
    "stack_public": [],
}
_DECKLIST = ["Mountain"] * 56 + ["Lightning Bolt"] * 4
_PASS = {
    "id": "pass", "kind": "pass", "public_cost": "",
    "public_target": "", "public_effect": "",
}


def grant(referee, *, refs=("secret-card-a", "secret-card-b"), **changes):
    args = dict(
        actor_id="actor",
        public_pile_id="exile-pile-1",
        state_revision=11,
        public_pile_size=len(refs),
        private_card_refs=refs,
        referee_authorized=True,
    )
    args.update(changes)
    return referee.grant_pile_choice(**args)


class Stage9COpaqueCapabilitiesTests(unittest.TestCase):
    def test_only_five_public_policy_fields(self):
        action = grant(BlindExilePileReferee())
        self.assertEqual(
            set(action),
            {"id", "kind", "public_cost", "public_target", "public_effect"},
        )

    def test_private_card_identities_not_exported(self):
        action = grant(
            BlindExilePileReferee(), refs=("Counterspell", "Black Lotus")
        )
        self.assertNotIn("Counterspell", repr(action))
        self.assertNotIn("Black Lotus", repr(action))

    def test_same_public_actions_across_secret_identity_swaps(self):
        a = grant(BlindExilePileReferee(), refs=("Counterspell", "Forest"))
        b = grant(BlindExilePileReferee(), refs=("Black Lotus", "Island"))
        self.assertEqual(a, b)
        result_a = visible_decision(
            own_decklist=_DECKLIST, public=_PUBLIC,
            observed_opponent_cards=["Island"],
            legal_visible_actions=[a, _PASS], policy_seed=7,
        )
        result_b = visible_decision(
            own_decklist=list(reversed(_DECKLIST)), public=_PUBLIC,
            observed_opponent_cards=["Island"],
            legal_visible_actions=[_PASS, b], policy_seed=7,
        )
        self.assertEqual(result_a, result_b)

    def test_one_concealed_card_is_still_an_opaque_pile(self):
        action = grant(BlindExilePileReferee(), refs=("secret",))
        self.assertIn("(1 cards)", action["public_target"])
        self.assertNotIn("secret", repr(action))

    def test_unauthorized_selection_rejected(self):
        with self.assertRaises(PermissionError):
            grant(BlindExilePileReferee(), referee_authorized=False)

    def test_qualifying_casting_cannot_use_blind_path(self):
        with self.assertRaises(PermissionError):
            grant(BlindExilePileReferee(), quality_restricted=True)

    def test_lookable_card_requires_a_separate_visible_path(self):
        with self.assertRaises(PermissionError):
            grant(BlindExilePileReferee(), actor_may_look=True)

    def test_invalid_flag_not_treated_as_authorization(self):
        with self.assertRaises(PermissionError):
            grant(BlindExilePileReferee(), referee_authorized=1)

    def test_bad_pile_size_rejected(self):
        with self.assertRaises(ValueError):
            grant(BlindExilePileReferee(), public_pile_size=3)

    def test_empty_pile_rejected(self):
        with self.assertRaises(ValueError):
            grant(BlindExilePileReferee(), refs=())

    def test_duplicate_private_card_refs_rejected(self):
        with self.assertRaises(ValueError):
            grant(BlindExilePileReferee(), refs=("same", "same"))

    def test_identity_derived_or_invalid_pile_id_is_rejected_when_not_public_id(self):
        with self.assertRaises(ValueError):
            grant(BlindExilePileReferee(), public_pile_id="hidden name with spaces")

    def test_duplicate_capability_rejected(self):
        referee = BlindExilePileReferee()
        grant(referee)
        with self.assertRaises(ValueError):
            grant(referee)

    def test_cost_must_be_paid_before_referee_randomness(self):
        referee = BlindExilePileReferee()
        action = grant(referee)
        attempted = []
        def draw(_size):
            attempted.append(True)
            return 0
        with self.assertRaises(PermissionError):
            referee.resolve_private(
                actor_id="actor", action_id=action["id"],
                state_revision=11, costs_confirmed_paid=False,
                referee_randbelow=draw,
            )
        self.assertFalse(attempted)
        self.assertEqual(
            "secret-card-a",
            referee.resolve_private(
                actor_id="actor", action_id=action["id"],
                state_revision=11, costs_confirmed_paid=True,
                referee_randbelow=draw,
            ),
        )
        self.assertEqual(1, len(attempted))

    def test_random_index_is_bounded_by_private_pile_size(self):
        referee = BlindExilePileReferee()
        action = grant(referee)
        draws = []
        def draw(n):
            draws.append(n)
            return n - 1
        result = referee.resolve_private(
            actor_id="actor", action_id=action["id"],
            state_revision=11, costs_confirmed_paid=True,
            referee_randbelow=draw,
        )
        self.assertEqual([2], draws)
        self.assertEqual("secret-card-b", result)

    def test_invalid_random_choice_rejected_without_spending_capability(self):
        referee = BlindExilePileReferee()
        action = grant(referee)
        with self.assertRaises(ValueError):
            referee.resolve_private(
                actor_id="actor", action_id=action["id"],
                state_revision=11, costs_confirmed_paid=True,
                referee_randbelow=lambda n: n,
            )
        self.assertEqual(
            "secret-card-a",
            referee.resolve_private(
                actor_id="actor", action_id=action["id"],
                state_revision=11, costs_confirmed_paid=True,
                referee_randbelow=lambda _: 0,
            ),
        )

    def test_wrong_actor_cannot_redeem(self):
        referee = BlindExilePileReferee()
        action = grant(referee)
        with self.assertRaises(PermissionError):
            referee.resolve_private(
                actor_id="opponent", action_id=action["id"],
                state_revision=11, costs_confirmed_paid=True,
            )

    def test_stale_revision_rejects_and_invalidates(self):
        referee = BlindExilePileReferee()
        action = grant(referee)
        with self.assertRaises(PermissionError):
            referee.resolve_private(
                actor_id="actor", action_id=action["id"],
                state_revision=12, costs_confirmed_paid=True,
            )
        with self.assertRaises(PermissionError):
            referee.resolve_private(
                actor_id="actor", action_id=action["id"],
                state_revision=11, costs_confirmed_paid=True,
            )

    def test_successful_capability_is_single_use(self):
        referee = BlindExilePileReferee()
        action = grant(referee)
        referee.resolve_private(
            actor_id="actor", action_id=action["id"],
            state_revision=11, costs_confirmed_paid=True,
            referee_randbelow=lambda _: 0,
        )
        with self.assertRaises(PermissionError):
            referee.resolve_private(
                actor_id="actor", action_id=action["id"],
                state_revision=11, costs_confirmed_paid=True,
            )

    def test_revocation_blocks_outstanding_ability(self):
        referee = BlindExilePileReferee()
        action = grant(referee)
        referee.revoke_all()
        with self.assertRaises(PermissionError):
            referee.resolve_private(
                actor_id="actor", action_id=action["id"],
                state_revision=11, costs_confirmed_paid=True,
            )

    def test_policy_rejects_private_metadata_injected_into_action(self):
        referee = BlindExilePileReferee()
        action = grant(referee)
        action["actual_secret_card"] = "Black Lotus"
        with self.assertRaises(ValueError):
            visible_decision(
                own_decklist=_DECKLIST, public=_PUBLIC,
                observed_opponent_cards=[], legal_visible_actions=[action, _PASS],
            )

    def test_invalid_pile_references_rejected(self):
        with self.assertRaises(ValueError):
            grant(BlindExilePileReferee(), refs=("a", ""))
        with self.assertRaises(ValueError):
            grant(BlindExilePileReferee(), refs=["a", "b"])

    def test_wrong_revision_type_rejected(self):
        with self.assertRaises(ValueError):
            grant(BlindExilePileReferee(), state_revision=True)


if __name__ == "__main__":
    unittest.main()
