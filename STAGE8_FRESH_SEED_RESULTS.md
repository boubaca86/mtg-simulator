# Stage 8 — fresh-seed replication results

Status: completed; **the predeclared positive-replication threshold was not met**.
`promotion_allowed=false`; no learned model controls Forge gameplay.

Public counts reduced seed-macro game-macro normalized regret by **0.00402**,
below the required **0.01000**. It improved regret in three of four families and
increased the corresponding top-choice accuracy by 1.95 percentage points, but
all three criteria were required. Secondary representations do not replace this
primary contrast after the results are known.

## Frozen experiment and operational deviation

The successful [32-game run](https://github.com/boubaca86/mtg-simulator/actions/runs/37396331864)
used capture commit `a3a9436a832502c6ba92432a95c7d0e77c2c95cc`, Forge 2.0.15,
three information-set worlds, and the four predeclared families 20261008–20261011,
each with four games in both deck orientations. All six models trained once on
the frozen 518-decision development corpus from families 20261004–20261007;
none of the fresh families entered training.

The [first attempt](https://github.com/boubaca86/mtg-simulator/actions/runs/37365982753)
failed the whole-log timeout contract and produced no model comparison. The
retry increased the CLI cutoff from 180 to 600 seconds. This is an operational
deviation from the original instruction to retain capture settings, not an exact
replication of that setting. The longest completed game took 359.983 seconds.
The same seeds, decks, features, trainer and success thresholds were retained;
the incomplete original source log was not mixed into the retry corpus.

## Coverage and paired metrics

All 32 games are represented: **450/641 captured proposals (70.20%)**, containing
1,742 candidates and 3,377 strict candidate pairs. There are 433 decisions with
strict label differences and 17 with all labels tied; 14 decisions contain
terminal-scale scores. This measures captured search proposals, including phase
probes that may be deferred, rather than coverage of all actual moves or priority
windows.

Top-choice accuracy accepts any maximum-score candidate and averages uniformly
over model ties. Lower normalized regret is better. Each fresh family represents
eight games, so equal-game and seed-macro game-macro averages coincide here.

| Model | Decision accuracy | Pairwise accuracy | Decision regret | Game-macro accuracy | Game-macro regret |
| --- | ---: | ---: | ---: | ---: | ---: |
| Uniform | 39.39% | 50.00% | 0.46234 | 39.67% | 0.46974 |
| Frozen legacy action hash | 56.47% | 56.28% | 0.33990 | 57.07% | 0.34054 |
| Canonical action only | 82.91% | 74.21% | 0.11417 | 80.59% | 0.13115 |
| Public counts | 85.41% | 72.71% | 0.10525 | 82.54% | 0.12713 |
| Visible identity | 88.30% | 72.92% | 0.07583 | 87.07% | 0.08581 |
| Public semantics | 86.80% | 73.70% | 0.09017 | 85.24% | 0.10333 |

The visible-identity secondary model performed best on accuracy and normalized
regret in this sample. That observation does not turn the failed primary
comparison into a positive replication or establish improved win rate.

| Fresh family | Public-count regret improvement over action only | Top-choice change |
| --- | ---: | ---: |
| 20261008 | -0.09901 | -12.67 pp |
| 20261009 | +0.02282 | +3.24 pp |
| 20261010 | +0.00065 | +0.54 pp |
| 20261011 | +0.09161 | +16.69 pp |
| Equal-family mean | +0.00402 | +1.95 pp |

## Verification and provenance

The complete six-model report reproduced byte for byte locally. Re-parsing and
serializing all eight original source logs also reproduced the counterfactual
corpus byte for byte. Every log passed both whole-game parsers, including
timeouts, exceptions, replay mismatches, strategy-fusion and three-world audit
checks. There are no unrepresented games.

Seventeen relevant source files, including all model/feature/training modules,
capture patches and deck exporter, are byte-identical between the frozen model
commit `71e6ab6b403cd889c60bb5743a31931e25366ff5` and the capture commit. The typed
target repair was developed separately and was not part of this experiment.

Hashes, exact metrics, ties, per-family results, per-game proposal coverage,
source-log diagnostics, deck hashes and code hashes are retained in
`results/forge_expert_ai_stage8/fresh-seed-replication-v1.json`.
The full original report and logs remain in workflow artifact `11383832643`.

- Development corpus SHA-256: `106912cfd8800cad8abe65231abeca03578c4185205a2e11b19e87000cc8ee7e`.
- Fresh corpus SHA-256: `3bb4addb85dbfb202e571988fab84309e8ce029d2e609560c86be14207cc8371`.
- Artifact SHA-256: `0a87c28d78a0702d376d7fb3487798ac6d0705fff79b637ec820baf392c4ebd2`.

These seeds are now observed. Any model changes informed by these results require
new untouched families for another confirmatory comparison. The labels still
imitate Forge's fixed-root scores; opponent-based and execution-aware evaluation
remain necessary before claiming playing-strength gains.
