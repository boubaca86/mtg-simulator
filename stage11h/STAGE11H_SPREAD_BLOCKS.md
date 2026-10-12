# Stage 11H: spread defensive blocks across visible threats

The live development baseline is Stage 11G, commit
`6fc060852c948c0ed20811581457c63eda23f1f3`. Its inspected run
[38264176093](https://github.com/boubaca86/mtg-simulator/actions/runs/38264176093)
passed 106 pinned Forge methods. It already supports two independent actors
casting, attacking and blocking with Grizzly Bears, Runeclaw Bear and Balduvian
Bears in naturally shuffled 60-card games. Earlier PRs #26 and #27 remain separate
and unmerged; this branch builds directly on the live Stage 11G code.

## Gameplay correction

The previous actor listed every target for the first blocker before listing the
next blocker. Forge accepts each blocker's first legal preference, so three
available blockers could all block the first attacker, allowing two other 2/2s
through. At one life this loses despite sufficient legal blockers. With eight
visible targets, the eight-pair limit could exclude every blocker after the first.

The actor now offers distinct first-choice targets across its defensive reserves,
then rotates through alternatives until the existing eight-pair limit is reached.
Only its approved own cards, visible opponent table and life totals are inputs.
Forge still decides actual attacker eligibility, tapped/sick restrictions,
blocking legality, damage, deaths and outcomes. No new observation fields,
privileged AI fallback, mana shortcuts or damage changes are introduced.

This is a bounded preference improvement, not an optimal matching algorithm.
Unknown readiness and the existing eight-pair cap can still leave a legal block
unproposed. More blockers than attackers may still produce multiple blocks on one
attacker. Menace and first-strike regressions retain their separate existing actors.

## Required evidence

- Exact 108 Forge methods: all 106 existing methods plus two focused regressions.
- Arranged three-versus-three fixture at one defending life: three distinct legal
  blocks, all six 2/2s die through native Forge combat, defender survives at one.
- Arranged fallback fixture: Forge rejects a tapped blocker and nonattacking
  targets, then accepts a later legal target without disclosing readiness.
- Existing naturally shuffled game cohorts continue unchanged; their results are
  separate from the arranged fixtures and are not evidence of arbitrary-deck skill.
- Eight real-worker Python protocol/policy tests; thirteen evidence-checker tests.
- The exact pre-change actor fails the two new local coverage checks as expected.
- Stage 10D/10E process/privacy checks and Stage 10H's 56 malformed JSON cases.

Three inherited forged-command tests previously caught their own `Assert.fail`.
Their final assertions now sit outside the catch, so accepting an invalid reply
fails the test. A disposable-source CI mutation disables the validator after the
successful 108-method baseline. It must produce exactly those three assertion
failures, each containing `FORGED_PAYLOAD_ACCEPTED:`; zero tests, skips, crashes,
unrelated failures and a surviving mutation fail the evidence gate. The source is
restored afterward. The XML checker also rejects wrong classes and duplicate,
missing, unexpected, failed or skipped methods.

Local Python checks passed (21 tests). Pinned Forge CI is required before this PR
is marked ready; exact commit, run and artifact evidence will be recorded in the
PR description. Successful tests and independent PR review are required before
merging. This development does not merge earlier pending PRs.

## Scope and research

Forge remains pinned to `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`
(`forge-2.0.15`) and runs under Java 21. The actors remain separate processes with
filtered observations, not hardened OS sandboxes. The decks and policies remain
deliberately narrow, not competitive expert Magic or the custom Ultimatus format.
No protected Stage 8P evaluation seeds, training or strength claims are involved.

Consulted [Wizards' current rules page](https://magic.wizards.com/en/rules) and
the [September 25, 2026 Comprehensive Rules](https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt),
especially 509.1a (legal blocking choices), 509.1h (blocked status), and 510.1b-d
(unblocked and blocked damage). These support leaving combat legality and damage
with the referee; spreading preferences is an actor policy, not a new game rule.
