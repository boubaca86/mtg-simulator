# Stage 8I — bounded control replay result

Status: **passed**.

Workflow run \`37466817528\` at commit
\`a78ed773533672f6e8d5de7a58c7c51740bb1ff0\` passed the complete Stage 8I
two-pass replay-equivalence gate.

Artifact \`11415482348\`:
SHA-256 \`e8ce24b2d4949f7a73ce6a015b1602e07d313e57960187a9ad68035b061afe14\`.

## Result

Across eight baseline games and eight externally requested replay games using the
already-observed seed \`20261012\`:

- 178 baseline complete-action captures produced 178 external requests;
- 178/178 requests were consumed exactly once;
- 178/178 controlled capture payloads reproduced the baseline exactly;
- every requested action was already in Forge's captured complete legal candidate set;
- every Stage 8I request equaled Forge's own baseline selection;
- 0 uncaptured/invalid requests were accepted;
- 0 different-from-Forge requests were accepted;
- 120 returned actions reproduced exactly;
- 120 controller-acceptance events reproduced exactly;
- 120 terminal lifecycle events reproduced exactly;
- all 8 normalized game results reproduced exactly;
- the direct Java regression accepted an exact captured identity and rejected an
  uncaptured identity fail-closed;
- Forge 2.0.15 remained the sole legality/rules referee;
- no card rules, costs, X values, modes, targets, choices, stack behavior or
  resolution semantics were changed.

\`promotion_allowed=false\`. This is control-path/safety evidence, not new
playing-strength evidence.

## Validation repairs

The first CI attempt failed before Forge because the request serializer wrote
literal \`\\t\` / \`\\n\` characters. That representation bug was corrected
without changing the protocol.

The second attempt reached the final audit and failed only because the
game-result normalizer matched a literal \`\\d\` rather than elapsed-time digits.
Artifact inspection showed that all eight winners, all 178 captures, all 120
returned/controller events and all 120 terminal events were already identical.
The regex was corrected and the full workflow was rerun successfully.

Neither repair changed seeds, decks, model weights, Forge version, candidate
selection, card semantics or pass thresholds.

## Next justified stage

Stage 8J may now test a **single learned-policy action intervention per game** on
new disjoint seed families. The learned model may request only an exact complete
Forge-captured legal action. All later choices in that game remain Forge-controlled.

The first learned-control experiment must be predeclared, side-specific, use
fresh seeds, preserve the hidden-information boundary, keep pass/defer behavior
under Forge, and compare against a matched Forge-only baseline before any broader
learned control is permitted.
