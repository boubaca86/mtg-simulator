# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 71,000
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.48% | 87.66% | +5.82 pp | 93.72% | 64.95% |
| Eradicator S.T | 93.18% | 88.48% | +4.70 pp | 95.09% | 58.13% |
| Drop Pod S.T | 93.03% | 88.67% | +4.36 pp | 93.33% | 57.60% |
| Pyroclast Squad S.T | 92.85% | 88.97% | +3.87 pp | 93.67% | 64.16% |
| Terminator S.T | 92.75% | 89.14% | +3.61 pp | 93.27% | 65.53% |
| Exterminatus S.T | 89.23% | 92.16% | -2.93 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.25% | 92.31% | -2.06 pp | 93.06% | 4.20% |
| Whirlwind S.T | 91.16% | 92.17% | -1.02 pp | 93.41% | 47.82% |
| Infernus S.T | 91.85% | 90.93% | +0.92 pp | 93.01% | 61.39% |
| Servo-Skull | 91.28% | 91.60% | -0.33 pp | 91.36% | 21.96% |
| Demolitionist S.T | 91.63% | 91.34% | +0.29 pp | 93.31% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
