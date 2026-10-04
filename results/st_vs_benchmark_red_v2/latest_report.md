# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 2,000
**S.T win rate:** 92.95%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Eradicator S.T | 94.70% | 89.56% | +5.14 pp | 96.51% | 58.75% |
| Pyroclast Squad S.T | 94.45% | 89.83% | +4.62 pp | 94.92% | 65.95% |
| Exterminatus S.T | 89.62% | 93.85% | -4.22 pp | 0.00% | 0.00% |
| Servitor | 93.98% | 90.84% | +3.14 pp | 94.21% | 65.65% |
| Terminator S.T | 94.05% | 90.92% | +3.13 pp | 94.35% | 64.55% |
| Vindicator S.T | 91.02% | 94.04% | -3.02 pp | 94.25% | 4.35% |
| Servo-Skull | 91.25% | 93.45% | -2.21 pp | 91.16% | 22.05% |
| Drop Pod S.T | 93.65% | 91.62% | +2.04 pp | 94.16% | 57.35% |
| Demolitionist S.T | 93.51% | 91.96% | +1.55 pp | 95.24% | 47.30% |
| Whirlwind S.T | 92.67% | 93.42% | -0.75 pp | 94.62% | 47.40% |
| Infernus S.T | 93.04% | 92.79% | +0.25 pp | 93.77% | 60.95% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
