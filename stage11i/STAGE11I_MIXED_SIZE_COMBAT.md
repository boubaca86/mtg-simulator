# Stage 11I: bounded 3/3 creature support and mixed-size combat

This branch builds on Stage 11H commit
`bb966c35310e1af09ae4b697753b8816c5cd9140`, [PR #28](https://github.com/boubaca86/mtg-simulator/pull/28).
That unmerged parent passed 108 real Forge methods and 21 Python checks in both
push and PR CI. Its independent review requirement remains in force.

## Change and purpose

The prior reciprocal actor/referee supported three named vanilla 2/2 creatures.
Stage 11I adds **Centaur Courser**, a target-free vanilla **2G 3/3**. Forge checks
the actual mana cost, sorcery timing, stack/priority, sickness, attack/block
legality, combat damage, toughness and deaths. The adapter permits the new name
and its exact printed combat damage; unsupported sources and damage values
still stop explicitly instead of invoking inherited full-information AI.

The independent actor proposes a Courser cast before cheaper creature fallbacks.
The referee may reject an unaffordable proposal and consider the next candidate.
The actor estimates incoming damage using only printed power for supported
face-up names, prioritizes the largest visible threats, and reserves enough
creatures to cover those threats if their blocks are legal. Stage 11H's distinct
first-target ordering and eight-pair cap remain intact.

There are no new observation fields or card-database access. Printed stats are a
small fixed policy table, used only when a supported card is in the actor's own
hand/table or has appeared face up on the opponent's table. The actor receives no
opponent decklist, hidden hand, library order, actual attacker list, readiness,
phase, turn, stack, or priority. Face-down objects do not acquire an identity or
stats. The estimate does not account for modifiers or hidden readiness.

The damage adapter preserves its existing fixed allocation: all damage goes to
the first referee-supplied legal combat target. It now admits exactly two damage
for the supported Bears or three for Courser, matching actual Forge damage. It
does not introduce tactical damage search or claim optimal multi-block play.

## Validation required before readiness

The exact method gate requires **114 real Forge tests**, retaining all 108
existing methods and adding:

1. Actual three-Forest mana payment, real stack resolution, summoning sickness,
   insufficient-mana denial, wrong-color denial and out-of-turn denial.
2. A native unblocked Courser attack dealing three damage, leaving 17 life.
3. Native 3/3-versus-2/2 combat in both roles: the Bear dies, Courser survives
   with two marked damage, and blocking prevents damage to the defending player.
4. Exact damage bounds and unsupported/own-target rejection without mutation.
5. Two naturally shuffled mixed-size games with the left player registered first.
6. Two naturally shuffled mixed-size games with the right player registered first.

The focused mana/combat tests are **arranged fixtures**, not claims about natural
opening hands. The four full games use actual registered 60-card decks:
48 Forest, four Grizzly Bears, four Runeclaw Bear and four Centaur Courser.
Forge performs native startup, London mulligans, normal 20 life and all turns.
No hand, library order, mana or battlefield is prepared. No seeds, game outcomes
or retries are selected. Actual Courser resolutions/allocations, blocks and
attacks are reported even if zero; correctness of the new behavior is guaranteed
by the focused fixtures, not probabilistic draws in the natural cohorts.

Local checks passed: **29 Python tests** (13 evidence-checker, eight Stage 11H
worker tests, eight new mixed-size worker tests). CI also retains the existing
Stage 10D/10E privacy checks, Stage 10H's 56 malformed-message cases, and the
Stage 11H disabled-validator mutation requiring exactly three expected assertion
failures. Exact commit, successful run and artifact details will be recorded in
the PR description before marking it ready.

## Limits and sources

This is a bounded additional creature capability, not expert search, general
Magic support, the user's custom Ultimatus rules or an AI strength claim. The
actors are separate persistent processes with filtered inputs, not hardened OS
sandboxes. Earlier pending PRs are not merged by this work. No protected Stage 8P
evaluation seeds, training or deployment are used.

Checked the exact pinned Forge
[Courser definition](https://github.com/Card-Forge/forge/blob/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676/forge-gui/res/cardsfolder/c/centaur_courser.txt)
and the official [Comprehensive Rules](https://magic.wizards.com/en/rules):
302.6 (sickness), 509.1 (blocking), 510.1 (damage), 601.2 (casting/payment),
and 704.5g (lethal marked damage). Forge remains pinned to
`4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676` under Java 21.

Successful tests and independent review are required before either stacked PR
is merged. Nothing in this stage relaxes that gate.
