# Mana-preserving London mulligans, integrated through Stage 12D

This integrates development through Stage 12D
`2306becd48746c187cd2d5fe3215d2821b1504a5` with the tested cleanup and evidence
work in [PR #32](https://github.com/boubaca86/mtg-simulator/pull/32), head
`5e9126636c31883baff5dd048ac74c416b04bc1d`, and its parent
[PR #31](https://github.com/boubaca86/mtg-simulator/pull/31).
Those pull requests remain unmerged. This branch carries both histories so
the current Flying, Deathtouch, Visionary, two-Elf defense and cleanup behavior are preserved.

## Behavior

The old opening ranking returned every supported creature before any basic
land. It could therefore keep excess lands while bottoming its only payable
creature. The reciprocal worker now protects the cheapest supported creature
that the basics in its own hand can pay for, together with the required lands.
Forests are preferred for green requirements, including Baloth's two green
mana. The supported costs cover Llanowar Elves, the three vanilla Bears,
Centaur Courser, Rumbling Baloth, Elvish Visionary, Scryb Sprites and Sedge Scorpion.

Unsupported spells, surplus basics and other creatures precede the protected
group. Original hand indices break ties, including duplicate card names.
Every prefix outside the group can be returned while retaining its complete
creature and mana. If more cards must be returned, that guarantee no longer
applies. With no payable supported creature, the existing own-hand ranking is
used. This is a bounded heuristic, not a claim of optimal play or strength.

The keep/mulligan decision is unchanged. The worker receives no mulligan
count, phase, priority, actual library order or opponent private cards.
Cleanup continues to use its separate own-hand ranking. Forge validates the
worker's full index permutation and alone determines the return count and
moves the actual physical cards.

## Verification and integration

The independent XML gate requires exactly **169 native Forge methods**:
164 inherited integration regressions and five new methods:

- Two independent opening workers return identical choices while the
  opponent's hidden hand and both library orders change.
- Every return count from zero through seven preserves exact physical hand
  and ordered library identities using native `LondonMulligan.mulliganDraw()`.
- Mixed-color Baloth hands retain two Forests and four total basics for every
  prefix that can preserve the complete group.
- After retaining two Forests and the original Bear, native turns play lands,
  pay mana, put that original Bear on the stack and resolve it onto the board.

The first four are explicitly arranged registered-card fixtures. The London
fixture overrides only `tuckCardsDuringMulligan()` to supply a fixed test
count; it does not prove Forge computed that count through repeated natural
mulligans. The actual draw, controller callback and bottom movements are
native. Existing uncurated `Match.startGame` cohorts separately exercise
natural shuffles, mulligans, paid casting and real outcomes without hand,
deck, seed or outcome selection.

Thirty-one Python checks cover opening preservation, nine supported creature
names across all seven cyclic hand layouts and every safe prefix, duplicate
indices, color requirements, independent processes, own-hand-only behavior,
cleanup and strict evidence handling. The inherited disabled-validator
mutation check must still produce exactly its three expected assertion
failures. CI evidence, commit identity and artifact digests are recorded in
the pull request after execution; static inventory checks are not native test
results.

The integrated source keeps PR #31's physical Elf damage checks, actual
combat-or-deck-out outcome validation and repaired rejection assertions, and
PR #32's strict worker bounds and cleanup regressions. The two additional
upstream arranged Elf-combat fixtures use the same explicit priority-pass
fixture control as the earlier combat fixtures, keeping the declared board
stable while native combat proceeds. Their separate natural games retain
normal priority decisions.

The initial push CI exposed an inherited keep-callback boundary error during a
natural Visionary game: the already-applied London return count was compared
with the remaining hand size. Four returned cards leave three in hand, so that
comparison rejected a valid native callback. The adapter now bounds the keep
count by Forge's maximum; actual bottom selection retains its separate physical
hand-size check. A fifth regression drives seven successive native
`LondonMulligan.mulligan()` calls without overriding the counter, checks every
reduced-hand callback and all original card IDs, and rejects negative or
above-maximum counts before IPC. This is an explicitly driven sequence, not a
natural keep-policy trajectory. Natural games are not retried for favorable
outcomes.

## Limits and references

This is still a limited ordinary-Magic simulator, not complete Magic or
Ultimatus. Processes share their execution environment; this is not hardened
OS isolation. No protected evaluation seeds, training or strength promotion
are used. Successful tests and independent pull-request review remain
required before merging.

- [Official London mulligan announcement](https://magic.wizards.com/en/news/announcements/london-mulligan-2019-06-03).
- [Pinned Forge LondonMulligan](https://github.com/Card-Forge/forge/blob/4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676/forge-game/src/main/java/forge/game/mulligan/LondonMulligan.java).
