# Stage 10Y — VERIFIED: true 20-life Forge outcome from completely uncurated natural hands

## Exact inspected push-CI evidence

- **Tested software SHA**: `8ce30207906526e640098d3ace2dea1fc339356b`, UNMERGED `expert-stage9a-forge-decision-pairs`.
- **Exact-software-SHA successful PUSH run** https://github.com/boubaca86/mtg-simulator/actions/runs/38240357564, completed SUCCESS attempt 1.
- **Inspected real job** `114781150121`: Maven exit 0, BUILD SUCCESS. Genuine pinned `forge.ai.Stage10UUncuratedDrawTest` TestNG **90 tests run, 0 failures, 0 errors, 0 skipped**.
- **Independent Surefire XML method-name verifier**: `{"executed_methods":90,"full_magic_game":false,"gate":"PASS",...}`, exactly 90 named and independently detected genuine Forge methods; all 88 inherited Stage10X tests also pass.
- **Pinned Card-Forge/forge** annotated `forge-2.0.15` peeled `4ec5f1a2c32fa90ecb983a72b9eb47aa5c5d7676`; exact checkout pin appears in passing Actions logs.
- **Stage10D / 10E actor IPC privacy**: two independent persistent external actor processes and legal/public/own observations PASS. **Stage10H strict JSON**: four actor types and 56 malformed JSON negatives PASS.
- **Evidence artifact** https://github.com/boubaca86/mtg-simulator/actions/runs/38240357564/artifacts/11742897705 ; `stage10y-natural-win-38240357564-1`, **86,799 bytes**, saves Maven log, per-test XML, original source hashes and independent gate.
- No merge, training, promotion, deployment, or protected Stage8P seed use. This follow-up documentation-only SHA is not itself separately software-tested.

## One functional gameplay increment

Two added real native pinned Forge TestNG methods:
1. `nativeUncuratedTwentyLifeForgeVictoryAttackerRegisteredFirst`
2. `nativeUncuratedTwentyLifeForgeVictoryDefenderRegisteredFirst`

With exactly two separate, persistent Python actor operating-system processes and one trusted authoritative Forge referee, both players register legal genuine 60-card Constructed decks; the attacking player's list contains 56 Forest and four Grizzly Bears and the intentionally nonblocking opposing player has 60 Islands. This is a **deliberately easy, passive benchmark opponent, not a skilled AI contest**. `Match.startGame` provides actual shuffles, London mulligans and completely untouched naturally dealt opening hands, then real draws, mana-paid Forest land use, priority/stack and attacks. No hand contents arranged, battlefield Bear inserted, life total adjusted, or library order disclosed. Both start at Forge's ordinary 20 life and only Forge sets/recognizes the natural combat outcome. Naturally occurring one-to-four Bear attacks deal legal even amounts of unblocked combat damage across multiple native turns; zero or lower defender life triggers native LifeReachedZero state-based loss. Genuine game outcome recognizes the registered attacking winner; card inventories remain conserved; both player registration orders passed independently.

The actors only see the objective, own private hand, own legally visible table, frozen original own registered deck names/counts, opponent's public visible table and Forge-owned public current life totals. They never receive opponent secret hand/library/deck or deck order/top, face-down identity, turn/phase/priority/stack or raw referee objects. Two OS PIDs are not hardened sandboxing.

## Limits and next concrete priority

This is a complete natural-card-draw, native-life-outcome **single game in a highly simplified matchup**, not a competitive full Magic match, general AI planning, arbitrary constructed/custom cards, trained search, or Ultimatus. Stage10Z should make a natural-game full outcome with **real opponent creature interactions** (e.g. a naturally sampled defending deck that casts and blocks first, followed by a legal winner), or introduce materially meaningful spell/response play. Avoid endless IPC hardening. Add new real Forge TestNG methods with independent name gate and inspect exact push SHA CI evidence.

## Research references

- Wizards official September 25 2026 Comprehensive Rules https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt : 100.2a, 103, 104.3b, 119.6, 302.6, 400.2, 509–510, 704. 
- Cowling, Powley & Whitehouse (2012), *Information Set Monte Carlo Tree Search*, https://doi.org/10.1109/TCIAIG.2012.2200894.
- Never run protected Stage8P evaluation seeds `20261042–20261049` or `20261050–20261057`.
