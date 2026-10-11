"""Stage 9C: referee-private blind exile-pile selection capabilities.

This is a Python boundary prototype, NOT a Forge integration or a proof that
a particular MTG action is legal. An authoritative Forge referee MUST determine
permissions, pile membership, costs, and the applicable Comprehensive Rules.

Rule 406.4: an actor without look permission selects a *pile*, not an
individual concealed card. The referee chooses randomly within that pile
after any required costs are paid. A quality-constrained 406.3b casting
action must not use this blind-choice path.

The policy receives ONLY the five Stage8Z public action fields. The card
references, legality verdict, and random selection never enter its inputs.
"""
from __future__ import annotations

from dataclasses import dataclass
import re
import secrets
from typing import Callable

_PUBLIC_PILE_ID = re.compile(r"[A-Za-z0-9_-]{1,48}\Z")
_ACTION_KIND = "choose-blind-exile-pile"
_PUBLIC_EFFECT = "Referee randomly selects one card after required costs are paid"


@dataclass(frozen=True, slots=True)
class _PrivateGrant:
    actor_id: str
    revision: int
    private_cards: tuple[str, ...]


class BlindExilePileReferee:
    """Only a trusted game-engine adapter may instantiate/use this object.

    Do NOT pass the referee or the private result of resolve_private() to AI.
    Public IDs must be supplied from the actor-visible exile-pile chronology,
    not derived from the hidden card's identity, type, cost or abilities.
    """

    def __init__(self) -> None:
        self._grants: dict[tuple[str, str], _PrivateGrant] = {}

    def grant_pile_choice(
        self, *,
        actor_id: str,
        public_pile_id: str,
        state_revision: int,
        public_pile_size: int,
        private_card_refs: tuple[str, ...],
        referee_authorized: bool,
        actor_may_look: bool = False,
        quality_restricted: bool = False,
    ) -> dict[str, str]:
        """Issue a public capability; trusted engine supplies all arguments.

        referee_authorized MUST be an independently checked legal-action
        verdict. This class intentionally never guesses legal moves from
        hidden cards or searches the Magic card catalog.
        """
        if not isinstance(actor_id, str) or not actor_id:
            raise ValueError("invalid actor")
        if not isinstance(public_pile_id, str) or not _PUBLIC_PILE_ID.fullmatch(public_pile_id):
            raise ValueError("invalid publicly assigned pile ID")
        if type(state_revision) is not int or state_revision < 0:
            raise ValueError("invalid state revision")
        if type(public_pile_size) is not int or public_pile_size <= 0:
            raise ValueError("invalid public pile size")
        if not isinstance(private_card_refs, tuple) or len(private_card_refs) != public_pile_size:
            raise ValueError("private pile does not match public count")
        if any(not isinstance(ref, str) or not ref for ref in private_card_refs):
            raise ValueError("invalid referee-private card reference")
        if len(set(private_card_refs)) != len(private_card_refs):
            raise ValueError("duplicate referee-private card reference")
        if type(referee_authorized) is not bool or not referee_authorized:
            raise PermissionError("referee has not authorized this selection")
        if type(actor_may_look) is not bool or type(quality_restricted) is not bool:
            raise ValueError("invalid viewing or qualification flag")
        if actor_may_look:
            raise PermissionError("lookable cards require a separate visible-card path")
        if quality_restricted:
            raise PermissionError("quality-restricted casting requires look permission")

        # Stable ONLY in public coordinates: two identical information sets
        # produce the same action ID regardless of private card identities.
        action_id = f"blind-pile:{public_pile_id}:revision:{state_revision}"
        key = (actor_id, action_id)
        if key in self._grants:
            raise ValueError("capability already issued for this actor and revision")
        self._grants[key] = _PrivateGrant(actor_id, state_revision, private_card_refs)
        return {
            "id": action_id,
            "kind": _ACTION_KIND,
            "public_cost": "",
            "public_target": f"face-down exile pile {public_pile_id} ({public_pile_size} cards)",
            "public_effect": _PUBLIC_EFFECT,
        }

    def resolve_private(
        self, *,
        actor_id: str,
        action_id: str,
        state_revision: int,
        costs_confirmed_paid: bool,
        referee_randbelow: Callable[[int], int] | None = None,
    ) -> str:
        """Engine-only! Return a PRIVATE card reference, never a policy value.

        A legal Forge casting pipeline is responsible for validating and paying
        costs first. Random selection happens only after that confirmation.
        No card is revealed here: the rules engine controls subsequent reveal.
        """
        if not isinstance(actor_id, str) or not isinstance(action_id, str):
            raise ValueError("invalid capability request")
        key = (actor_id, action_id)
        grant = self._grants.get(key)
        if grant is None:
            raise PermissionError("unissued or consumed capability")
        if type(state_revision) is not int or grant.revision != state_revision:
            self._grants.pop(key, None)
            raise PermissionError("stale capability")
        if type(costs_confirmed_paid) is not bool or not costs_confirmed_paid:
            raise PermissionError("referee has not confirmed costs paid")
        choose = referee_randbelow if referee_randbelow is not None else secrets.randbelow
        index = choose(len(grant.private_cards))
        if type(index) is not int or not 0 <= index < len(grant.private_cards):
            raise ValueError("invalid referee random choice")
        self._grants.pop(key)
        return grant.private_cards[index]

    def revoke_all(self) -> None:
        """Call whenever actor-visible game state or legality changes."""
        self._grants.clear()
