# Stage 8C — typed-target development result

Status: development comparison passed its correctness gates and improved all
three ranking metrics. This is not a promotion result; learned gameplay remains disabled.

## Verified live run

Successful workflow run: `37399134353` at capture commit
`c3add5b439c9907cea3c4b7e1f45c9e694584564`.

The run rebuilt pinned Forge 2.0.15, applied the fixed-root information-set
patches, passed the Stage 6 complete-action regression and the real-Forge typed
target boundary regression, then captured the same four development families
20261004–20261007 in both deck orientations.

Coverage remained 518 rankable decisions from 750 captured proposals across 31
represented games (69.07%). The Stage 8B public-semantics baseline reproduced
its previous numbers exactly.

| Representation | Top-choice | Pairwise | Normalized regret |
| --- | ---: | ---: | ---: |
| Stage 8B public semantics | 81.08% | 70.91% | 0.09218 |
| Stage 8C typed target semantics | 82.63% | 72.41% | 0.08209 |
| Change | +1.54 pp | +1.50 pp | -0.01009 |

The improvement is consistent across the three development metrics, but these
families were used during feature development. It therefore cannot authorize
model promotion or shadow/live control by itself.

## Safety result

The typed resolver no longer parses display names such as `Ai(2)` to infer
object identity. Forge snapshots typed target references internally and resolves
them only against the legal public root state. Opponent hand and libraries are
never scanned for target features. Raw Forge object IDs do not cross the learner
boundary. Exact executable recipes remain unchanged for auditing and replay.

No custom card text, rules or gameplay semantics were altered.

## Next gate

The original Stage 8B fresh families 20261008–20261011 failed their own
predeclared primary threshold and are now observed. Stage 8C therefore uses a
new predeclared confirmation set: 20261012–20261015. See
`STAGE8C_FRESH_SEED_PROTOCOL.md`.

A positive Stage 8C replication permits only the next shadow-policy experiment:
the model may score/log Forge candidates while Forge still selects and executes
every action. It does not authorize learned move control.
