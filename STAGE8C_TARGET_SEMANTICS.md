# Stage 8C — Public target semantics

Status: development-only offline ranking experiment. No learned model controls Forge gameplay.

## Motivation

Stage 8B deliberately canonicalizes complete Forge recipes by removing transient
object IDs. That is necessary for reusable learning, but it makes two physical
objects with the same visible name indistinguishable even when their public
characteristics differ. A removal spell aimed at a 1/1 Goblin and the same spell
aimed at a 5/5 Goblin can therefore collapse to the same learning action.

Stage 8C repairs that representation without weakening the information-set
boundary.

## Referee boundary

Forge remains the sole source of legality, card behavior, targets, modes, X,
choices and fixed-root counterfactual scores.

Transient Forge object IDs are permitted only inside Forge while resolving the
already-selected target against legally visible zones. The learner-facing
capture never serializes those IDs. It receives only public descriptors:

- legal public zone;
- public controller role (self/opponent/public);
- visible card name, or an opaque marker for face-down objects;
- public mana value and type;
- current public power/toughness for creatures.

The resolver never scans the opponent hand or either library. An object that
cannot be resolved through the legal public zones becomes an opaque unresolved
target rather than causing hidden state to be inspected.

## Compatibility

The existing Stage 8A schema remains readable. target_public_semantics is an
optional candidate field so the frozen Stage 8 fresh-seed replication remains
reproducible on its original commit and corpus. Stage 8C itself fails closed if
fresh target semantics are absent.

Existing complete action identities and exact execution recipes are unchanged.
No custom card text or gameplay semantics are modified.

## Learning comparison

stage8c_target_ranker.py performs the same leave-one-seed-family-out evaluation
used by Stage 8B and compares:

1. Stage 8B public-semantic context/action interactions;
2. the same representation plus candidate-specific public target semantics.

The comparison reports top-choice accuracy, strict pairwise accuracy and
normalized regret on identical held-out decisions. promotion_allowed remains
false regardless of the result.

The target-aware representation earns further study only if it improves held-out
decision quality without weakening the anti-cheating and exact-action contracts.
A development improvement is not a promotion gate; a later untouched corpus is
still required.

## Safety regressions

The Stage 8C tests require that:

- same-name targets with different public characteristics can be distinguished;
- Stage 8B remains unable to exploit transient object IDs;
- raw object IDs are rejected at the learner boundary;
- missing target semantics fail closed for Stage 8C;
- player-only targets may legitimately have no card descriptor;
- target-aware fitting remains seed-family isolated and offline.
