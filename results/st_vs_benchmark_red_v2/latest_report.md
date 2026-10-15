# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 129,500
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.67% | +5.88 pp | 93.83% | 64.99% |
| Eradicator S.T | 93.16% | 88.70% | +4.47 pp | 95.06% | 57.84% |
| Drop Pod S.T | 93.00% | 88.88% | +4.12 pp | 93.28% | 57.50% |
| Terminator S.T | 92.84% | 89.12% | +3.72 pp | 93.34% | 65.53% |
| Pyroclast Squad S.T | 92.83% | 89.12% | +3.71 pp | 93.64% | 64.44% |
| Exterminatus S.T | 88.97% | 92.31% | -3.34 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.33% | 92.35% | -2.02 pp | 92.69% | 4.24% |
| Infernus S.T | 92.01% | 90.76% | +1.25 pp | 93.14% | 61.58% |
| Whirlwind S.T | 91.20% | 92.24% | -1.03 pp | 93.46% | 47.87% |
| Servo-Skull | 91.25% | 91.68% | -0.43 pp | 91.29% | 21.93% |
| Demolitionist S.T | 91.64% | 91.47% | +0.17 pp | 93.28% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
