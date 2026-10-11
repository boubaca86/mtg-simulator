# Stage 9F — Wall of Mourning real Forge effect canary

**Status (2026-10-11): Executed and GREEN for a disposable, patched Forge
checkout. This is not a production change or a full AI/privacy proof.**

## Canonical rationale

- Official MTG Comprehensive Rules effective 2026-09-25: 406.3a/b,
  406.4, 406.6–7, 607.2a, 708.5 and 601.2h–i:
  https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt
- Official WotC Innistrad: Midnight Hunt release notes, Wall of Mourning:
  players cannot inspect its exiled cards; when there is more than one,
  choose at random even if their prior identity was known:
  https://magic.wizards.com/en/news/feature/innistrad-midnight-hunt-release-notes
- Hidden-information ISMCTS framing: Cowling, Powley & Whitehouse (2012),
  https://eprints.whiterose.ac.uk/id/eprint/75048/

## Real pinned implementation and test

- Upstream Card-Forge/forge tag `forge-2.0.15`, peeled commit:
  `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Original card script at
  `forge-gui/res/cardsfolder/w/wall_of_mourning.txt` has a hidden
  `ChangeZone`, `Card.ExiledWithSource`, `ChangeNum$ 1`, but no
  `AtRandom`. With no `AtRandom`, pinned `ChangeZoneEffect` can
  ask AI to pick among the actual private cards.
- Branch `expert-stage9a-forge-decision-pairs`:
  - `forge_tests/WallOfMourningForgeEffectTest.java`: two TestNG tests
    load the **actual card SVar** via Forge's `AbilityFactory`, and
    one calls the **actual `ChangeZoneEffect` resolver** with two linked
    genuine Forge face-down exile cards plus an unlinked negative control.
  - `forge_tests/verify_stage9f_surefire.py`: strict XML gate for
    exactly one intended baseline failure and exactly two patched passes.
  - `forge_patches/stage9f_wall_of_mourning_random_choice.patch`:
    unapplied **candidate**, adds `AtRandom$ True` only to the
    return effect; no production patch or upstream push.
  - `.github/workflows/stage9f-forge-wall-of-mourning.yml`:
    read-only, branch-scoped pinned checkout, separate baseline and
    disposable patched Maven/TestNG execution, saved logs, XML, hashes.

## Verified execution evidence

GitHub Actions: https://github.com/boubaca86/mtg-simulator/actions/runs/38131153265

- Feature branch commit: `cfed90a27e2fc46e611b493f6a885533f895a33c`.
- Baseline gate: **PASS**, exact one *expected red*
  `returnAbilityRequiresBlindRandomSelection`, matching the
  missing-random assertion; the baseline is NOT a passing Forge test.
- Disposable patched Forge: Maven exit **0**, `BUILD SUCCESS`,
  **Tests run: 2, Failures: 0, Errors: 0, Skipped: 0**;
  strict patched XML gate **PASS**, executed 2.
- Artifact: `stage9f-forge-wall-evidence-38131153265-1`,
  id `11699570309`. Includes baseline and patched Maven logs,
  Surefire XML, gate JSON, patch diff and pinned source hashes.
- Local verifier-only XML accept/reject scenarios: 6/6;
  **not** interchangeable with the live Forge results.

## Limitations (still blockers to production)

1. The positive test invokes the real named card **return effect
   directly**. It does **not** exercise normal triggered ability timing,
   real ETB exile from library, Coven eligibility, three independent
   players' turns, priority, stack or effect on a complete match.
2. `AtRandom` uses Forge's `Aggregates.random`; these two cases do
   **not** prove statistically uniform output or RNG ownership in
   production, and do not instrument controller callback noninvocation.
3. The test has two linked genuine face-down Forge exile cards but
   sets up their linkage as a trusted fixture. It does not verify
   full historical `DigEffect` provenance and re-exile incarnation.
4. The test does not assess actor-visible policy/action/telemetry
   noninterference across permuted hidden card identities, including
   previously known library tops, foretell or adventure.
5. Wall's triggered return has no cost; this test cannot prove
   pay-before-reveal costs, illegal-cast rollback, or 406.3b qualified
   concealed casting.
6. Stage 9E's cross-pile aliasing and stale permission/live-auth gaps
   remain separate and unresolved; Stage9A visibility-only patch
   remains unapproved.
7. Independent reviewer sign-off is still required before any patch,
   AI integration, games, training or reserved-seed evaluation.

## Next executable gate

Add a proper **three-player ETB → linked face-down exile → Coven end-step
trigger** integration fixture against pinned upstream Forge. Compare
unmodified baseline and disposable candidate; independently check
uniform blind choice using referee-side observations, not actor-card
identities. Instrument AI choice callback noninvocation and make paired
actor-visible observations identical when concealed identities are
permuted. Preserve exact test XML and negative controls, and obtain
independent review before production work.

No Stage9A merge, training, gameplay/evaluation seeds, or deployment
occurred in Stage9F.
