# Stage 11O: critical mixed-creature defense

This change starts from live development commit
`721a5ce3be2b9ba1fe0b07ef3d460b08e3ef90d9` (Stage 11N). Its baseline
[Forge run](https://github.com/boubaca86/mtg-simulator/actions/runs/38299714411)
passed all 120 named methods. Stage 11O's exact tested commit, CI results,
and artifact digest are recorded in its pull request after CI completes.
Successful tests and independent PR review are required before merging.

## Gameplay change

At lethal risk, the previous actor listed all targets for one blocker before
listing the next blocker. Forge accepts the first legal pair for each blocker,
so all blockers could pile onto one attacker while other attackers dealt lethal
damage. The first round of critical-life preferences now covers distinct public
threats, largest first. Later rounds rotate the targets as legality fallbacks,
within the existing eight-pair limit.

The reserve calculation now subtracts each known threat's actual printed power
(2, 3, or 4) until the remaining potential damage is strictly below the actor's
life total. For example, against a 4/4 and a 3/3 at four life, one legal block of
the 4/4 would suffice. At three life, both threats need blocking. The actor no
longer reserves too many creatures by treating every block as two prevented
damage. Healthy-life cooperative-block and solo-block preferences are preserved.

These remain conditional preferences. Readiness, actual attackers, phase,
priority, stack, hidden cards and library order are not actor inputs. Only the
existing known vanilla creature names are evaluated. Unknown creatures,
abilities, power changes, and illegal or truncated proposals can invalidate
the estimate. This is neither a survival guarantee nor a general combat solver.
Forge still authorizes every actual action and determines damage and outcomes.

## Verification

- Three new Forge methods plus all 120 inherited methods, using upstream Forge
  `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676` (`forge-2.0.15`).
- Two explicit registered-card fixtures, one per registration order: three
  2/2 defenders at one life stop separate 4/4, 3/3 and 2/2 attackers. The test
  checks three distinct native blocks, survival at one life, the physical
  identities of all three dead blockers and the traded 2/2 attacker, damage on
  both surviving attackers, and preserved full registered card inventories.
- A separate arranged fallback fixture: Forge rejects a tapped blocker and
  nonattacking targets, then accepts a later legal pair without revealing those
  private readiness facts to either process.
- Twelve real-worker Python tests check mixed-power reserve thresholds,
  distinct-first proposals, pair caps, healthy cooperation, opening requests,
  separate process IDs, and rejection of unapproved or ambiguous input.
- Thirteen synthetic evidence tests check the exact-name XML gate and mutation
  evidence. They are reported separately from gameplay tests.
- Three inherited rejection assertions are repaired so they cannot catch their
  own failure. CI disables the validator only in its disposable Forge checkout;
  all three selected methods must fail with the expected rejection marker.
  The original source is restored afterward.

The arranged regressions are not naturally shuffled games. The inherited
natural cohorts remain uncurated and run to Forge outcomes; no event is inferred
from a lucky draw and no unsuccessful natural game is rerolled. No protected
evaluation seed, training run, merge, or strength promotion is part of this work.
Two separate persistent Python processes are not hardened OS isolation.
Full Magic and the custom Ultimatus scoring rules remain outside this stage.

## Primary references

- [Official Comprehensive Rules](https://magic.wizards.com/en/rules), effective
  [2026-09-25 text](https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt):
  509.1a (one attacking creature per ordinary blocker), 510.1b-d (combat damage).
- [Pinned Forge combat implementation](https://github.com/Card-Forge/forge/blob/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676/forge-game/src/main/java/forge/game/combat/Combat.java).
