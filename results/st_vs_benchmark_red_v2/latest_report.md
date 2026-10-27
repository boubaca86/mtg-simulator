# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 272,500
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.63% | +5.80 pp | 93.72% | 64.96% |
| Eradicator S.T | 93.04% | 88.65% | +4.39 pp | 94.98% | 57.81% |
| Drop Pod S.T | 92.92% | 88.79% | +4.13 pp | 93.16% | 57.40% |
| Terminator S.T | 92.80% | 88.93% | +3.87 pp | 93.29% | 65.46% |
| Pyroclast Squad S.T | 92.73% | 89.06% | +3.67 pp | 93.54% | 64.29% |
| Exterminatus S.T | 88.92% | 92.20% | -3.28 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.32% | -2.20 pp | 92.39% | 4.24% |
| Infernus S.T | 91.91% | 90.69% | +1.23 pp | 93.03% | 61.35% |
| Whirlwind S.T | 91.11% | 92.14% | -1.03 pp | 93.50% | 47.81% |
| Servo-Skull | 91.07% | 91.61% | -0.54 pp | 91.10% | 22.03% |
| Demolitionist S.T | 91.57% | 91.33% | +0.24 pp | 93.24% | 47.06% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
