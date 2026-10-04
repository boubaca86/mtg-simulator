# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 500
**S.T win rate:** 92.40%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servo-Skull | 86.67% | 94.21% | -7.54 pp | 86.55% | 23.80% |
| Pyroclast Squad S.T | 94.69% | 87.58% | +7.11 pp | 95.74% | 65.80% |
| Vindicator S.T | 88.20% | 94.72% | -6.52 pp | 95.45% | 4.40% |
| Servitor | 93.79% | 89.04% | +4.74 pp | 94.43% | 68.20% |
| Eradicator S.T | 93.98% | 89.29% | +4.69 pp | 96.21% | 58.00% |
| Terminator S.T | 93.71% | 89.76% | +3.95 pp | 94.28% | 66.40% |
| Whirlwind S.T | 91.00% | 94.50% | -3.50 pp | 93.89% | 45.80% |
| Drop Pod S.T | 93.53% | 90.58% | +2.95 pp | 93.73% | 54.20% |
| Exterminatus S.T | 90.60% | 92.95% | -2.35 pp | 0.00% | 0.00% |
| Infernus S.T | 92.86% | 91.57% | +1.28 pp | 93.53% | 61.80% |
| Demolitionist S.T | 92.04% | 93.17% | -1.13 pp | 94.78% | 49.80% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
