# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 137,500
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.70% | +5.84 pp | 93.82% | 65.01% |
| Eradicator S.T | 93.13% | 88.76% | +4.37 pp | 95.06% | 57.87% |
| Drop Pod S.T | 93.03% | 88.84% | +4.19 pp | 93.29% | 57.42% |
| Terminator S.T | 92.85% | 89.11% | +3.74 pp | 93.36% | 65.53% |
| Pyroclast Squad S.T | 92.83% | 89.15% | +3.67 pp | 93.63% | 64.43% |
| Exterminatus S.T | 88.93% | 92.32% | -3.39 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.30% | 92.38% | -2.08 pp | 92.73% | 4.23% |
| Infernus S.T | 92.00% | 90.80% | +1.21 pp | 93.15% | 61.56% |
| Whirlwind S.T | 91.20% | 92.26% | -1.07 pp | 93.50% | 47.85% |
| Servo-Skull | 91.24% | 91.69% | -0.45 pp | 91.28% | 21.92% |
| Demolitionist S.T | 91.65% | 91.46% | +0.19 pp | 93.30% | 47.23% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
