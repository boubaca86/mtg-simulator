# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 32,000
**S.T win rate:** 91.61%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.73% | +5.82 pp | 93.82% | 65.14% |
| Eradicator S.T | 93.32% | 88.43% | +4.89 pp | 95.09% | 58.47% |
| Drop Pod S.T | 93.10% | 88.74% | +4.36 pp | 93.41% | 57.83% |
| Pyroclast Squad S.T | 92.93% | 89.07% | +3.86 pp | 93.68% | 64.04% |
| Terminator S.T | 92.65% | 89.59% | +3.05 pp | 93.12% | 65.48% |
| Exterminatus S.T | 89.28% | 92.25% | -2.96 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.43% | -2.19 pp | 92.54% | 4.10% |
| Infernus S.T | 91.93% | 91.00% | +0.94 pp | 93.09% | 61.59% |
| Whirlwind S.T | 91.37% | 92.03% | -0.67 pp | 93.66% | 47.73% |
| Demolitionist S.T | 91.80% | 91.26% | +0.54 pp | 93.52% | 47.33% |
| Servo-Skull | 91.32% | 91.69% | -0.37 pp | 91.39% | 21.99% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
