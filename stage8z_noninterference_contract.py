"""Stage 8Z: adversarial observable-decision boundary, not a live Forge AI.

The referee must supply an independently validated, actor-visible legal-action
list. This module NEVER derives legal moves from hidden cards or the global
card catalog. It intentionally does not attempt to solve full MTG strategy.
"""
from __future__ import annotations
import hashlib
import json
from stage8x_information_view import information_view, fingerprint

ALLOWED_ACTION_FIELDS = frozenset({"id", "kind", "public_cost", "public_target", "public_effect"})
def _canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=True)

def visible_decision(*, own_decklist, public, observed_opponent_cards, legal_visible_actions, policy_seed=0):
    if type(policy_seed) is not int:
        raise ValueError("policy_seed must be a fixed independent integer")
    view = information_view(own_decklist=own_decklist, public=public,
                            observed_opponent_cards=observed_opponent_cards)
    if not isinstance(legal_visible_actions, list) or not legal_visible_actions:
        raise ValueError("A trusted visible legal action list is required")
    ids = set()
    actions = []
    for action in legal_visible_actions:
        if not isinstance(action, dict) or set(action) != ALLOWED_ACTION_FIELDS:
            raise ValueError("Unexpected action field; hidden-state metadata prohibited")
        if not all(isinstance(action[k], str) for k in ALLOWED_ACTION_FIELDS):
            raise ValueError("Action fields must be public strings")
        if not action["id"] or action["id"] in ids:
            raise ValueError("Action IDs must be unique and nonempty")
        ids.add(action["id"])
        actions.append(dict(action))
    actions.sort(key=lambda a: a["id"])
    # Deliberately non-strategic deterministic baseline. The digest depends
    # solely on visible observations, permitted actions, and independent seed.
    digest = hashlib.sha256(_canonical({
        "observation": fingerprint(view),
        "actions": actions,
        "policy_seed": policy_seed,
    }).encode("utf-8")).digest()
    index = int.from_bytes(digest[:8], "big") % len(actions)
    return {
        "observation_fingerprint": fingerprint(view),
        "visible_action_set_fingerprint": hashlib.sha256(_canonical(actions).encode("utf-8")).hexdigest(),
        "selected_action_id": actions[index]["id"],
        "policy": "stage8z-visible-deterministic-baseline-NOT-live-Forge",
    }
