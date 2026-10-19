# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 178,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.50% | 87.67% | +5.83 pp | 93.79% | 64.95% |
| Eradicator S.T | 93.12% | 88.66% | +4.46 pp | 95.03% | 57.86% |
| Drop Pod S.T | 92.98% | 88.83% | +4.15 pp | 93.23% | 57.48% |
| Terminator S.T | 92.80% | 89.10% | +3.70 pp | 93.29% | 65.46% |
| Pyroclast Squad S.T | 92.79% | 89.11% | +3.69 pp | 93.58% | 64.37% |
| Exterminatus S.T | 88.95% | 92.26% | -3.31 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.35% | -2.13 pp | 92.64% | 4.21% |
| Infernus S.T | 91.98% | 90.73% | +1.24 pp | 93.12% | 61.47% |
| Whirlwind S.T | 91.16% | 92.20% | -1.04 pp | 93.49% | 47.79% |
| Servo-Skull | 91.13% | 91.67% | -0.54 pp | 91.16% | 22.02% |
| Demolitionist S.T | 91.63% | 91.39% | +0.24 pp | 93.25% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
