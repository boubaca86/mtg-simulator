# Stage 9D: Forge-backed blind exile-pile boundary canary

**Status:** test-only integration boundary **verified** against pinned
Card-Forge/forge tag `forge-2.0.15`. It does **NOT** make the production
Forge AI legally blind-cast exiled cards or prove global information safety.

## Research basis

- [Official Magic Comprehensive Rules](https://magic.wizards.com/en/rules),
  current rules dated September 25, 2026: 406.3a, 406.3b, 406.4, 708.5.
  Rule 406.4 distinguishes a choice of a publicly identifiable pile from
  an illegal choice of a specific concealed card, with delayed disclosure
  until the required cost has been paid.
- [Pinned Forge `Card.java`](https://github.com/Card-Forge/forge/blob/forge-2.0.15/forge-game/src/main/java/forge/game/card/Card.java)
  maintains `mayPlay` permission separately from `mayPlayerLook` /
  `CardView.canBeShownTo`.
- [Pinned Forge `PlayerZone.java`](https://github.com/Card-Forge/forge/blob/forge-2.0.15/forge-game/src/main/java/forge/game/zone/PlayerZone.java)
  checks `mayPlay` when enumerating actor-accessible external-zone cards.
- [Pinned Forge `StaticAbilityMode.java`](https://github.com/Card-Forge/forge/blob/forge-2.0.15/forge-game/src/main/java/forge/game/staticability/StaticAbilityMode.java)
  lists `Continuous` as valid; `MayPlay` is a parameter, NOT a valid
  standalone mode. The first canary failed on this fixture error.
- [Information Set MCTS, Cowling–Powley–Whitehouse (2012)](https://eprints.whiterose.ac.uk/id/eprint/75048/)
  motivates grouping indistinguishable hidden states under one actor policy.

## What was implemented

- `forge_tests/BlindPileForgeBridgeTest.java`: six real Forge TestNG
  methods using actual `Game`, `Player`, `Card`, `CardView`,
  `PlayerZone` and a **test-only** private Java referee bridge.
- `forge_tests/verify_stage9d_surefire.py`: fails closed unless all
  six named methods truly executed, without errors, failures or skips.
- `.github/workflows/stage9d-forge-blind-pile-canary.yml`: branch-scoped
  **push-triggered**, read-only Java 21 + Xvfb + Maven build of upstream
  Forge 2.0.15 with only the isolated test class copied into the test tree;
  it never changes the production AI source or runs gameplay simulations.

The bridge makes the five-field public choice
`{id,kind,public_cost,public_target,public_effect}` while keeping actual
Forge `Card` references inside a private referee-only grant. It checks
actor ownership, state revision, single-use redemption, referee authorization,
no quality-restricted blind path, an unreadable face-down exile pile and
required-cost confirmation before referee random index selection.

## Actual evidence

- [Run #1](https://github.com/boubaca86/mtg-simulator/actions/runs/38104968082):
  **FAIL**, 6 executed, 5 passed, 1 failed. The fixture incorrectly used
  `Mode$ MayPlay` instead of a valid `StaticAbilityMode`.
  It is *not* evidence of an AI information leak.
- Fix commit `366e12fce08d4d9b4e29c4bc9e6122a502a86183`: use the
  supported `Mode$ Continuous` when constructing a synthetic permission
  token; the security assertions and six method names remain unchanged.
- [Run #2 — successful](https://github.com/boubaca86/mtg-simulator/actions/runs/38105075249):
  head commit `366e12fce08d4d9b4e29c4bc9e6122a502a86183`,
  **6 TestNG methods executed, 6 passed, 0 failures, 0 errors,
  0 skipped**, Maven BUILD SUCCESS, strict XML gate PASS.
  Evidence artifact `stage9d-forge-canary-38105075249-1`,
  artifact ID `11688879738` (Maven log, Surefire XML, source SHAs,
  verifier JSON), retained 14 days.

## Critical limitations and next integration gate

1. The Java bridge is nested inside a test class, not deployed as a
   `forge-game` or `forge-ai` production module.
2. The test's `refereeAuthorized` and `costsPaid` values are *trusted
   fixtures*; they are NOT validated by resolving an actual named Magic
   card effect or exercising Forge's full cast/land/action transaction.
3. A fabricated `StaticAbility` serves as the test's permission token.
   Real effect-based legality, hidden-pile grouping, costs and public
   action availability must be authorized independently inside Forge.
4. Public action IDs must be based only on public pile chronology and
   visible game-state revision, never private card IDs, costs or qualities.
5. Forge must retain the actual card choice and random draw inside its
   referee authority; policy input/telemetry must never include this card.
6. Add Forge TestNG acceptance cases with a real card/effect grant,
   look-permitted versus look-forbidden casting, 406.3b qualification,
   costs/rollbacks, and legal morph/foretell/adventure permissions.
7. Obtain independent review and a fresh passing build before production
   patch integration, any gameplay evaluation, seed consumption, or merge.

**No AI-vs-AI games, reserved seeds, training, or production merge occurred.**
