# Stage 9C — Referee-owned blind-exile capabilities (design + evidence)

**Status:** Python boundary prototype verified; **not integrated into Forge**.
No feature patch or Stage9A/Stage9B test fixture is authorized to merge based on this document.

## Primary source and approach

- [Official MTG Comprehensive Rules, effective 2026-09-25](https://magic.wizards.com/en/rules) (TXT):
  - 406.3a: an effect may permit play from face-down exile; reveal/turn face up
    before announcing a non-face-down play.
  - 406.3b: a player cannot cast a spell requiring particular qualities from
    face-down exile without being permitted to look at the relevant card.
  - 406.4: if the player cannot look at a specific face-down exiled card, they
    choose a *pile* and the actual card is randomly selected from that pile;
    for casting/activation, card identity stays hidden until required costs
    have been paid.
  - 708.5: opponent's face-down battlefield original face is not public.
- Pinned [Forge 2.0.15 PlayerZone.java](https://github.com/Card-Forge/forge/blob/forge-2.0.15/forge-game/src/main/java/forge/game/zone/PlayerZone.java),
  blob SHA `8559dd032a77d60c465cbeed7655c0c807a778a7`:
  `OwnCardsActivationFilter` tests private `mayPlay`, `mayPlayerLook`,
  keywords and raw spell abilities. Its `getCardsPlayerCanActivate`
  enumerates top-of-library and external zones.
- Pinned [Forge 2.0.15 Card.java](https://github.com/Card-Forge/forge/blob/forge-2.0.15/forge-game/src/main/java/forge/game/card/Card.java),
  blob SHA `7c6c6c929dc93166652d3ab868a6f3730387f496`:
  `getAllPossibleAbilities` may use the original state for face-down cards,
  and `mayPlay` permissions are separate from `mayPlayerLook`.
- Pinned [ComputerUtilAbility.java](https://github.com/Card-Forge/forge/blob/forge-2.0.15/forge-ai/src/main/java/forge/ai/ComputerUtilAbility.java),
  blob SHA `a833dfe88ea4809b10506e901f90f5ebe7302267`:
  raw candidate enumeration and `getAllPossibleAbilities` must not form
  the player-policy input on private identities.
- [Information Set Monte Carlo Tree Search (Cowling et al., 2012)](https://eprints.whiterose.ac.uk/id/eprint/75048/)
  explains why decisions must be based on information sets, rather than
  a perfect-information search over each imagined deal.

## Two design options considered

1. **Visibility-only filter:** simple, and the Stage9A/9B targeted Forge
   regressions pass, but it may suppress legally authorized blind selections.
2. **Referee-owned opaque capability (recommended):** Forge inspects actual
   private game data to determine legality; it publishes only actor-observable
   action descriptors. The AI chooses a public pile capability rather than a
   private card. Forge resolves the actual card after validated costs are paid,
   using referee randomness. More work is required to integrate correctly.

## Implemented prototype (NOT a legal-action engine)

`stage9c_opaque_capabilities.py` implements `BlindExilePileReferee`.
It requires an independent `referee_authorized=True` verdict supplied by a
trusted adapter; it **cannot determine Magic legality** and does not try to.
It exposes only the exact five Stage8Z public action fields: `id`, `kind`,
`public_cost`, `public_target`, `public_effect`. The action ID depends
only on public pile ID and public revision, never actual hidden cards.

The referee keeps private card references, validates actor binding and
revision, requires a trusted cost-paid confirmation before picking a random
index, rejects quality-constrained blind casting, and consumes selected
capabilities exactly once. Call `revoke_all()` on changes to action legality.
Only the **game engine** may see `resolve_private()` results; exposing them
to the policy defeats the firewall.

**Integration dependency:** The Java Forge adapter must assign public pile
IDs from actual exile events, derive public pile sizes, maintain an authoritative
revision, and validate permissions and costs. Policy-generated IDs, revision
values, permissions, or cost confirmations are untrusted. The adapter must
not put private card names, IDs, attributes, or unpublicized choice metadata
into any public field.

## Automated evidence and scope

- Previous Stage9A+9B run #4:
  [GitHub Actions evidence](https://github.com/boubaca86/mtg-simulator/actions/runs/38102118664);
  6 patched Stage9A methods pass plus 2 Stage9B battlefield methods pass.
- Stage9C Python boundary run:
  [GitHub Actions run](https://github.com/boubaca86/mtg-simulator/actions/runs/38102971094),
  head commit `af5406d7bbfe67efc9f2f676fd7fa38ce7ab33ce`;
  **23 executed, 0 failures**. Evidence artifact
  `stage9c-python-evidence-38102971094`.
- Stage9C Python tests are synthetic contract tests, **not** Java Forge
  integration, not gameplay simulations and not a global privacy proof.

## Required next Forge-specific acceptance tests

1. Effect grants play from face-down exile, **without** look permission:
   issue a public pile capability without exposing any private original face.
   Do not use this blind path for a quality-restricted 406.3b action.
2. Quality-restricted casting without look permission: reject the proposed
   blind action; adding look permission permits proper individually readable
   choices only if Forge itself authorizes the actual play.
3. Same actor-visible public state/piles but swapped concealed identities:
   identical public legal-action descriptions and policy decision.
4. Rule 406.4 costs/pile resolution: the independent game referee chooses
   uniformly within the selected public pile at the correct phase after
   costs are paid. A denied or stale capability cannot execute.
5. Face-down battlefield identity cannot leak through
   `Card.getAllPossibleAbilities` or zone activation filters, while ordinary
   public targeting and the controller's legal turn-up actions still work.
6. Verify actual TestNG method counts from Surefire XML; save logs, source
   SHAs and applied diff, separate baseline red and patched green.
7. Audit legal plays from other external zones, foretell/adventure,
   manifest/morph, and state/permission changes before production integration.

**Merge gate:** independent reviewer, new Forge legality tests, 100% expected
named methods executed, patched build green, no hidden-identity telemetry,
and explicit approval. No AI games, training or reserved evaluation seeds.

## Update: six Forge 9C permission methods LIVE verified (2026-10-11)

- [GitHub Actions run 38107635273](https://github.com/boubaca86/mtg-simulator/actions/runs/38107635273), head commit `ee3deeabc734b430ad89716558203c758fc71622`:
  pinned Forge tag `forge-2.0.15`, Java 21, Xvfb, disposable checkout.
- The proposed Stage9A visibility patch was applied **only within that disposable checkout**.
  No production Forge change, deployment, gameplay, reserved seeds, or merge.
- Real TestNG class `forge.ai.FaceDownExilePermissionBoundaryTest`:
  **6 executed, 6 passed, 0 failures, 0 errors, 0 skipped**; Maven exit 0,
  BUILD SUCCESS; strict Surefire XML method-name verifier PASS.
- Files: `forge_tests/FaceDownExilePermissionBoundaryTest.java`,
  `forge_tests/verify_stage9c_forge_exile_surefire.py`,
  `.github/workflows/stage9c-forge-exile-permissions.yml`.
- Evidence artifact `stage9c-forge-evidence-38107635273-1`, ID `11690219059`,
  includes Maven logs, XML, gate JSON, exact source hashes and proposed patch diff.
- These tests distinguish opponent/own concealed exile, explicit and persistent
  viewing permission, face-up exile, and **look permission versus play permission**.
  They do **not** test a legal blind pile transaction, spell-specific 406.3b
  authorization, costs/rollback or the production Forge policy interface.
- [Stage9D separate actual-Forge bridge canary run 38105075249](https://github.com/boubaca86/mtg-simulator/actions/runs/38105075249)
  passed six more Forge tests on unmodified pinned source, but its referee
  remains **test-only** and its authorization/cost confirmations are fixtures.

**Remaining merge gate:** independently verify referee-issued public pile IDs,
actual Forge effect legality and pile membership, costs and rollback,
noninterference of the production policy/telemetry, and downstream
face-down battlefield/foretell/adventure paths. Do not merge Stage9A or
promote this bridge based on synthetic or isolated canary tests alone.
