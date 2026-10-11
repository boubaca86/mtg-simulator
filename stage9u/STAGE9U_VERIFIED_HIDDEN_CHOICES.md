# Stage 9U — Hidden-choice fail-closed callbacks and exact registered-library inventory

**Status: verified GREEN in real pinned Forge**, October 11, 2026.

GitHub Actions: https://github.com/boubaca86/mtg-simulator/actions/runs/38143421993

- Feature branch `expert-stage9a-forge-decision-pairs`, validated source commit
  `7c6654bda43a40f7809c9140bd534e095e3ce17c` (**UNMERGED**).
- Pinned upstream Forge tag `forge-2.0.15` resolves to
  `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`.
- Maven **BUILD SUCCESS** / exit 0. Actual TestNG
  **26 executed, 26 passed, 0 failures, 0 errors, 0 skipped**.
  Independent strict Surefire XML checker reports `gate:PASS`.
- Evidence artifact `stage9u-guarded-hidden-choices-38143421993-1`,
  ID `11704001690` (Maven log, per-method XML, source and pinned hashes).
- Sources: `forge_tests/Stage9UGuardedHiddenChoicesTest.java`,
  `forge_tests/verify_stage9u_surefire.py`,
  `.github/workflows/stage9u-guarded-hidden-choices.yml`,
  unchanged external worker `stage9o/combat_actor.py`.

## Changes and demonstrated properties

**Six formerly inherited privileged decision callbacks** are now
explicitly denied by the test-only external Forge actor controller:

- `chooseCardsPile`: hidden-pile selection
- `chooseCardName`: both list-of-faces and predicate overloads
- `chooseCardsForZoneChange`: bulk hidden-zone selection
- `arrangeForScry`: library top/bottom ordering
- `arrangeForSurveil`: library/graveyard ordering

Every unsupported method immediately throws `SecurityException`
without inspecting concealed cards or delegating to Forge's
full-information `PlayerControllerAi` logic.
Adversarial tests pass actual opponent hand/library cards as
selection candidates, change concealed identity between trials, and
verify no AI process callback, private-state mutation, unauthorized
observation change, stack action or life change occurs.

**Exact test-fixture registered-to-physical inventory reconciliation:**
the trusted referee constructs 20 real registered main-deck cards for
*each* player directly as physical Forge library cards, independently
counts every card by name and ownership, and matches that count exactly
to its immutable registered starting-deck multiset. Genuine
`Player.drawCard()` draws one from each library, preserves all
registered card counts across library and hand, and changes only the
recipient's observation; the other isolated AI cannot see the drawn
card. The two different external Python workers receive only own
allowed card lists and referee-maintained life totals.

Twenty-three earlier Stage9T actual Forge gameplay/callback tests
remain green, including two-process priority decisions, genuine
land and creature casting, combat attacks and the previously guarded
non-priority callback families.

## Cautions and outstanding work

- This is a test-only **manual loading of the exact registered deck**,
  not a full authentic initial match/deck-construction/shuffle/mulligan
  workflow or a complete playable Magic match.
- Unexamined `PlayerControllerAi` non-priority callbacks still inherit
  privileges to inspect the referee's hidden state. Six more guarded
  families are a measurable improvement, **not a general security
  proof**. Full policy separation still requires replacing or
  comprehensively auditing that inherited controller.
- Separate Python processes are distinct but not hardened OS
  sandboxes; shared filesystem/network/debugging must be restricted
  before considering adversarial AIs secure.
- User's custom negative-life and library-exhaustion game-end format
  is not implemented by these stock Forge canaries.
- No evaluation seeds, policy training, main merge, unapproved
  Stage9A visibility patch, production deployment or promotion.

**Audit trail:** The first candidate run
`38143227967` contained a test assertion expecting an unsupported
worker digest response. The next run `38143261776` executed
**26/26 Maven tests green** but had a strict XML gate class-name
configuration typo. Both were corrected; the final run
`38143421993` passed both Maven tests and the independent gate.

## Suggested next Stage 9V

Replace inherited `PlayerControllerAi` with a minimum-privilege
controller or enforce an audited, explicit dispatch boundary for
all remaining non-priority callbacks. Add authenticated actor IPC,
harden separate process execution (filesystem/network),
and validate real opening hand/shuffle/mulligan decisions from
registered decklists before expanded target/combat strategies.

Research consulted: [Magic Comprehensive Rules effective September
25, 2026](https://magic.wizards.com/en/rules),
[pinned Forge 2.0.15 source](https://github.com/Card-Forge/forge/tree/forge-2.0.15),
[Cowling et al. (2012) ISMCTS](https://eprints.whiterose.ac.uk/id/eprint/75048/).
