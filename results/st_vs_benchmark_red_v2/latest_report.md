# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 279,500
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.44% | 87.63% | +5.81 pp | 93.73% | 64.97% |
| Eradicator S.T | 93.05% | 88.65% | +4.40 pp | 94.98% | 57.81% |
| Drop Pod S.T | 92.91% | 88.80% | +4.11 pp | 93.16% | 57.40% |
| Terminator S.T | 92.80% | 88.96% | +3.85 pp | 93.29% | 65.45% |
| Pyroclast Squad S.T | 92.75% | 89.06% | +3.69 pp | 93.55% | 64.29% |
| Exterminatus S.T | 88.93% | 92.20% | -3.27 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.12% | 92.33% | -2.21 pp | 92.30% | 4.24% |
| Infernus S.T | 91.92% | 90.69% | +1.23 pp | 93.05% | 61.34% |
| Whirlwind S.T | 91.12% | 92.14% | -1.01 pp | 93.49% | 47.81% |
| Servo-Skull | 91.07% | 91.62% | -0.54 pp | 91.10% | 22.04% |
| Demolitionist S.T | 91.57% | 91.35% | +0.23 pp | 93.24% | 47.06% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
