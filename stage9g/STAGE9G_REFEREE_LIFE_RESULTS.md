# Stage 9G — Referee-owned shared life totals (VERIFIED SYNTHETIC)

**Status, 2026-10-11:** real GitHub Actions run **38132891638**,
https://github.com/boubaca86/mtg-simulator/actions/runs/38132891638
at feature-branch SHA `c204d1cbd2f3b16814d7f67f7e20228135b75a54`:
15 executed Python unittest methods, 15 passes, **CI SUCCESS**, source hashes
recorded and evidence artifact `stage9g-life-contract-38132891638-1` (ID
11700325484).

## User-approved information interface

There are two actor processes, each receiving only *its own* serialized
observation. The trusted referee owns the private `RefereeMemory` and manages
both players' actual hands, library orders, table and life totals.

The observation includes **only**:
- The agreed game objective.
- Actor's own hand, own decklist **multiset** (never draw order), and own
  table cards only when legally identifiable.
- Opponent's cards actually played on the table, with opaque shells for
  uninspectable face-down cards.
- Exact referee-owned `life_totals.self` and `life_totals.opponent`, with
  their perspective reversed for the opponent.

The referee updates the totals after resolving effects and sends revised
observations to **both** actors. Invalid/partial referee updates fail closed.
Actor messages **cannot** update life; the only currently accepted test
command is `{"intent":"pass"}`. The protocol explicitly rejects hidden
hands, draw orders, turn/phase/priority, arbitrary zones, logs or unapproved
other state. Negative totals are allowed as raw numbers, but no winner or
game-over verdict is disclosed by this interface.

## What was tested

The branch files `stage9g/referee_life_protocol.py`,
`stage9g/test_referee_life_protocol.py`, and
`.github/workflows/stage9g-referee-life-contract.yml` cover:

1. Two-perspective initial life totals.
2. Changes after trusted life updates, visible to both actors.
3. Negative life without sending unapproved win-state metadata.
4. Actor-supplied life/damage command rejection, including duplicate JSON.
5. Atomic fail-closed referee updates and integer-type validation.
6. Defensive copies prevent dictionary mutation of referee life.
7–9. Opponent hand, own/opponent library draw order and concealed table
   identity noninterference.
10–14. Own legitimate changes, face-down permission, strict schema and
   opponent concealed identity rejection, decklist ordering invariance.
15. **Two distinct Python worker processes** with separately delivered
    observation bytes and updated life totals, but no referee object.

Pinned `Card-Forge/forge` tag `forge-2.0.15`, exact commit
`4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`, was checked out
and verified in CI **for provenance**, but **Forge gameplay was not run**.

## Remaining blockers

- The protocol is a **test-only** Python projection, not integrated into
  the actual Forge Player.getLife() change/resolution event stream; the
  method argument `totals_from_forge` must eventually be read from the
  authoritative Forge objects. It is not a production authorization token.
- Separate Python processes are not yet hardened against shared filesystem,
  OS memory/debugging, network access or side-channel leaks.
- No full Magic legal-action protocol, stack/priority/game-over signaling,
  callback provenance or authentic match execution is provided here.
- Stage9F's Wall of Mourning candidate remains unapproved for production;
  Stage9E stale grants, re-exile identity, cost and hidden-information gaps
  remain open.
- This work is not AI-versus-AI gameplay, policy training, evaluation seeds,
  or a basis for merging Stage9A.

### Research

Official Magic Comprehensive Rules September 25, 2026 rule 119.1–119.6
(life totals, effects and zero-life state-based loss), rules 406.3–406.7,
708.5:
https://magic.wizards.com/en/rules

Pinned Forge Player.java `Player.getLife()`,
`Card.java`, `CardView.java`, `PlayerZone.java`,
`ComputerUtilAbility.java` at Forge 2.0.15.

Cowling, Powley & Whitehouse (2012), *Information Set Monte Carlo Tree
Search*: https://eprints.whiterose.ac.uk/id/eprint/75048/

**Next task:** referee-only bridge from real Forge `Player.getLife()` to
both actor observations with callbacks after genuine Forge life events,
then read-only security CI and independent review, without widening either
actor's information.
