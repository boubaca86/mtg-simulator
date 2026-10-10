# Stage 8T — Controlled exploration protocol (proposal only)

## Decision
Do not dispatch new Forge games yet. Stage 8S observed 27/42 additional target/effect features never activated in 24 prior development games. This is selected-action sparsity, not proof the actions were legal or useful. Stage 8R's performance gate failed. The right next step is to pre-register safe data acquisition and instrument legal-action availability before collecting data.

## Phase A: instrument and audit, without gameplay
1. Pin Forge revision, card-definition JSON SHA-256, both exact decklist digests, baseline opponent and action parser. Preserve frozen Stage 8O and failed Stage 8P artifacts.
2. Inventory **all** historical workflow run manifests, paired game identifiers and seed families, checking for collisions and missing provenance. Permanently quarantine 20261032 and 20261033. Proposed 20261042–49 (development) and 20261050–57 (holdout) are NOT VERIFIED UNUSED.
3. Extend public-only telemetry to describe *all Forge-legal action candidates* (not only selected actions), with stable action IDs, phase, public target/effect features, mode and target choice, and legal candidate count. Preserve beneficial self-target gain-life and pump actions. Never expose private opponent information, future draws or full hidden decks.
4. Add synthetic regression fixtures for multi-target spells, variable X costs, self-pump, self-lifegain, self-damage, opponent damage, passes, priority windows, identical modes with different targets, and legal-action enumerator consistency.
5. Report availability coverage separately from selection coverage and completed-game outcome coverage. Unselected actions must NOT be labeled with observed game results.

## Phase B: precommitted exploration design; no automatic approval
- Objective: improve public-state and legal-action *coverage* in independent development games; not to maximize a retrospective win rate.
- Use Forge as sole rules referee. Sample exclusively from Forge legal actions; preserve exact rules, cards, decks, mana, stack and terminal audit.
- Frozen baselines: deterministic existing baseline, frozen Stage 8O model and a limited exploration policy. Choose a policy *before* the seed family is used.
- Exploration policy proposal: epsilon-greedy over **legal candidates**, with epsilon=0.10 for decisions where 2+ legal non-pass candidates exist; use a frozen baseline choice otherwise. Exploration draws uniformly over the remaining legal non-pass candidates, including self-targeting beneficial effects. This is only a PROPOSAL, not permission to deploy. Log actual selection probability and RNG provenance. Never force illegal moves or discard opponent safety.
- A pass must remain selectable whenever Forge permits it. Avoid infinite loops by an independently frozen action/turn/time budget; incomplete or unsafe games are failures, never silently discarded.
- Predefine seed families, 2 opposing orientations per family, sample size, maximum permitted failures, exact deck and Forge hashes, audit log schema, frozen opponent, stop rule, paired comparison, statistical confidence and negative result reporting BEFORE collection. Prevent duplicate dispatch with a persistent atomic one-shot seed-family reservation ledger.
- Split independent development and prospectively reserved held-out families *before* looking at outcomes. Never tune against reserved held-out outcomes; failed evaluation families remain consumed.
- Do not deploy this exploration policy to the final strength evaluation; it is for **development data acquisition only**.
- Review and approve Phase A telemetry and Phase B full protocol in separate PRs. No new games until both reviews, immutable manifests, safety gates and verified unused seeds.

## Analysis after approved development collection
Group validation by seed family (both orientations together). Compare frozen Stage 8O with a simple regularized supervised outcome model and action-coverage diagnostics before considering offline RL. Do not infer values for unchosen actions from a chosen-action terminal result. Measure coverage and calibration separately from prospective paired game strength. Do not retrospectively loosen any performance gate.

## Research basis
- Conservative Q-Learning, Kumar et al. (2020): https://arxiv.org/abs/2006.04779 — out-of-distribution action values are unreliable.
- Implicit Q-Learning, Kostrikov et al. (2021): https://arxiv.org/abs/2110.06169 — constrain improvement to supported behavior.
- Mildly Conservative Q-Learning, Lyu et al. (2022): https://arxiv.org/abs/2206.04745 — excessive conservatism also limits exploration.

This document is a design proposal. It does not claim improvement, authorize gameplay, select seeds or merge Stage 8R/8S.
