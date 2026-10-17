# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 158,000
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.50% | 87.75% | +5.75 pp | 93.79% | 64.98% |
| Eradicator S.T | 93.13% | 88.73% | +4.40 pp | 95.03% | 57.87% |
| Drop Pod S.T | 93.00% | 88.86% | +4.14 pp | 93.26% | 57.49% |
| Terminator S.T | 92.86% | 89.07% | +3.78 pp | 93.35% | 65.49% |
| Pyroclast Squad S.T | 92.83% | 89.10% | +3.73 pp | 93.63% | 64.43% |
| Exterminatus S.T | 88.97% | 92.29% | -3.32 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.26% | 92.38% | -2.12 pp | 92.83% | 4.22% |
| Infernus S.T | 92.00% | 90.76% | +1.25 pp | 93.14% | 61.53% |
| Whirlwind S.T | 91.16% | 92.28% | -1.12 pp | 93.49% | 47.79% |
| Servo-Skull | 91.18% | 91.69% | -0.51 pp | 91.22% | 22.04% |
| Demolitionist S.T | 91.68% | 91.38% | +0.29 pp | 93.32% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
