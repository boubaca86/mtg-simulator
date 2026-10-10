# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 74,000
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.50% | 87.65% | +5.84 pp | 93.73% | 64.96% |
| Eradicator S.T | 93.16% | 88.56% | +4.60 pp | 95.07% | 58.09% |
| Drop Pod S.T | 93.03% | 88.68% | +4.36 pp | 93.32% | 57.61% |
| Pyroclast Squad S.T | 92.84% | 89.00% | +3.84 pp | 93.67% | 64.15% |
| Terminator S.T | 92.75% | 89.15% | +3.60 pp | 93.27% | 65.58% |
| Exterminatus S.T | 89.30% | 92.16% | -2.86 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.34% | -2.11 pp | 92.87% | 4.21% |
| Whirlwind S.T | 91.15% | 92.20% | -1.05 pp | 93.43% | 47.82% |
| Infernus S.T | 91.88% | 90.89% | +0.99 pp | 93.03% | 61.37% |
| Demolitionist S.T | 91.65% | 91.32% | +0.33 pp | 93.32% | 47.25% |
| Servo-Skull | 91.28% | 91.61% | -0.33 pp | 91.35% | 21.98% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
