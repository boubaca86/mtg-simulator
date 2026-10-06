# Stage 8C — Public target semantics

Status: typed-target repair, real-Forge regression and corrected live ranking
capture passed. The predeclared fresh-seed gate also passed; see
`STAGE8C_FRESH_SEED_RESULTS.md` and the next step in `STAGE8D_SHADOW_POLICY.md`.
Development-only offline ranking; no learned model controls Forge gameplay.

The first resolver parsed numbers from display strings and could mistake
`Ai(2)` for card ID 2. Its unversioned target captures are retired. The repaired
boundary requires `target_semantics_version=forge-public-targets-v2`; see
`STAGE8C_TYPED_TARGET_REPAIR.md` for the failure, implementation and verification.

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
already-selected target against legally visible zones. Target descriptors never
serialize those IDs; exact executable action recipes retain their existing
identity for auditing. The target feature array receives only public descriptors:

- legal public zone;
- public controller role (self/opponent/public);
- visible card name, or an opaque marker for face-down objects;
- public mana value and type;
- current public power/toughness for creatures.
- an explicit player target and self/opponent role, without card characteristics.

The resolver never scans the opponent hand or either library. An object that
cannot be resolved through the legal public zones becomes an opaque unresolved
target rather than causing hidden state to be inspected.

## Compatibility

The existing Stage 8A schema remains readable. target_public_semantics is an
optional candidate field so the frozen Stage 8 fresh-seed replication remains
reproducible on its original commit and corpus. A supplied target feature array
must have the v2 marker and valid descriptors. Stage 8C fails closed if typed
target semantics are absent, obsolete or unversioned.

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
- player targets cannot be mistaken for card IDs or carry card characteristics;
- target order and multiplicity survive capture;
- unknown and face-down targets cannot acquire hidden characteristics;
- target-aware fitting remains seed-family isolated and offline.
