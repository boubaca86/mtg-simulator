# Stage 10S: validate native combat from twenty life

Development base: `f2afcfef10acdc5122808ca9af5c6c01e2135728` on
`expert-stage9a-forge-decision-pairs`. That head contains the two Stage 10S
scenarios and their initial verifier, but no Stage 10S CI workflow.
The last verified stage was 10R: [run 38194501517](https://github.com/boubaca86/mtg-simulator/actions/runs/38194501517),
76 real Forge tests, zero failures/errors/skips, at `30ebda6`.

While this candidate was being prepared, the development branch added its own
Stage 10S workflow in `6530a952e2d7c9cbd9e9f2f81ac86870700df787`.
This PR incorporates that commit and preserves its development-branch trigger
alongside PR validation and the additional integrity checks.

## What this change finishes

- Run all 78 named Stage 10S TestNG methods on Java 21 and the exact Forge
  `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676` release commit.
- Require both the ten-hit 20-to-zero game and the three-hit 20-to-14 ongoing
  game, alongside all 76 previous regressions. Each hit must occur on a distinct
  native Forge turn. Referee-only logs retain turn numbers and cumulative life;
  these logs are never actor observations.
- Preserve the two-process Stage 10D/E observation canaries and Stage 10H's
  four-actor-type strict JSON checks (56 malformed inputs).
- Reject missing, extra, duplicated, anonymous, failed, errored, skipped, or
  wrong-class XML results. Thirteen synthetic Python tests exercise the
  evidence verifier and mutation verifier; these are not gameplay tests.
- Repair three inherited rejection tests that caught their own `Assert.fail`.
  Validation and accepted actor payloads are unchanged. The repair matches
  the focused assertion fix in PR #23, now applied to the live Stage 10S copy.
- Disable only `validateReply` in the disposable Forge test copy after the
  baseline passes. Require exactly the three repaired methods to fail with
  `java.lang.AssertionError` and `FORGED_PAYLOAD_ACCEPTED:`. Compilation errors,
  zero executed tests, unrelated failures, skips, crashes, and surviving
  mutations cannot pass. Restore source in `finally`; archive mutation
  evidence separately from passing gameplay evidence.

The workflow runs on the dedicated PR branch and relevant pull requests. It
retains the software/Forge commit identities, source hashes, Python canaries,
Maven logs and exit codes, per-method XML, and both gate reports for 14 days.
Successful CI and independent review are required before merging.

## Scope and rules

The new scenarios use Forge's native 20-life start, registered sixty-card test
decks, a mana-paid Grizzly Bears spell, ordinary turn progression, combat, and
native state-based loss. They never set life or force game-over. The earlier
low-life fixtures remain separate regression tests.

These are deliberately controlled integration games: the attacker's hand is
curated from its registered physical cards; its deck contains one Bear and
59 basic lands; the opponent has 60 Islands and cannot contest combat.
They are not arbitrary shuffled-deck matches or evidence of strong strategy.
The actors run in distinct processes with restricted JSON observations, not
in a hardened operating-system sandbox. Other unsupported choices still stop.

The [official Comprehensive Rules](https://magic.wizards.com/en/rules), effective
September 25, 2026, establish the ordinary 20-life start (103.4), native combat
damage (510), and zero-life state-based loss (704.5a). This is ordinary Magic
engine validation. It does not implement Ultimatus's negative-life and
library-exhaustion scoring rules.

No protected seed games, training, promotion, or deployment are part of this
change. Seeds 20261032 and 20261033 remain consumed. Future reserved/candidate
families 20261042–20261057 are not used.

## Status

Local evidence checks and actor canaries pass. Pinned Forge CI is required
before this candidate can be marked verified. The PR remains unmerged until
independent review is complete.

Next after verification: remove curated opening-hand assumptions with a
bounded supported-deck game, including a supported discard decision if native
turn progression requires it, before expanding to competitive opponents.
