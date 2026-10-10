# Stage 8X — Observation-only boundary (provisional)

## Research and issue
Magic Comprehensive Rules (September 2026), rules 400/401/402: hand and library
are hidden, except explicitly revealed/visible information.
Source: https://magic.wizards.com/en/rules

Research: Cowling, Ward and Powley (2012), *Ensemble Determinization in
Monte Carlo Tree Search for the Imperfect Information Card Game Magic*:
https://eprints.whiterose.ac.uk/id/eprint/75050/
IS-MCTS research warns against planning using secret determinized facts:
https://eprints.whiterose.ac.uk/id/eprint/75048/

Existing `patch_forge_information_set.py` pools the opponent's real unknown
hand and *real hidden library* and shuffles those exact secret card objects.
This changes their positions, but not the hidden deck composition supplied to
the simulated search, so it violates a player-only information set.

## Implemented (safety boundary, not a learned planner)
- `patch_forge_stage8x_observed_only.py` switches Forge expert full/hybrid
  simulation mode to its **normal heuristic fallback** by default *for the
  patched build*. It also makes direct invocation of copied-world simulation
  throw, rather than silently use actual secret cards.
- Escape hatch `-Dforge.expert.observedOnly=false` exists strictly for
  reproducing historical builds and is UNSAFE for new learning/evaluation.
  **Never set it in future training or fresh reserved evaluation.**
- `stage8x_information_view.py` creates a separate observation-only policy
  input: own submitted deck multiset, own hand, public objects, opponent hidden
  zone COUNTS, and cards legitimately observed in this game. It accepts no
  opponent secret deck or global catalog of hypothetical opponent spells.
  Previously seen cards are history, not assertions about present hidden zones.
- The information view is diagnostic and **is not yet wired to live actions**.

## Critical limitations
- Fallback to the stock Forge heuristic is NOT a proof that every heuristic
  obeys information noninterference. Some game-rule effects legitimately access
  hidden zones; that must not be mistaken for AI knowledge.
- New Stage8X mode intentionally sacrifices full/hybrid rollout-based planning
  until a true information-set policy exists. Do not claim it is stronger.
- The Java guard can stop use of known contaminated copied-world search, but a
  successful build alone does not prove a complete opponent-hidden-information
  firewall or reliable memory of revealed cards.
- Existing historical Stage8P results remain frozen; do NOT rerun its consumed
  seed families 20261032 or 20261033. Candidate families 20261042–49 and
  20261050–57 are NOT VERIFIED and are NOT AUTHORIZED.
- Stage8X requires independent review, end-to-end behavioral noninterference
  tests, one-shot seed audit and prospectively frozen protocol before games,
  retraining, merging, or promoting a new AI.

## Acceptance next
1. Validate against Forge `forge-2.0.15` real Java build; no gameplay.
2. In a trusted test harness construct two legal games with same own deck,
   visible state and observed history, but different actual opponent hidden
   contents. Demand identical observed-only action choices with same RNG.
3. Implement a visible-state value function using own deck + observed opponent
   history, with explicit rules for revealed information after zone changes.
4. Prove action legality through Forge, test positive self-targeting, and only
   then pre-register development-data collection.
