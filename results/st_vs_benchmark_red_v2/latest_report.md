# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 123,000
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.69% | +5.87 pp | 93.84% | 64.98% |
| Eradicator S.T | 93.18% | 88.70% | +4.48 pp | 95.07% | 57.86% |
| Drop Pod S.T | 93.03% | 88.86% | +4.17 pp | 93.32% | 57.48% |
| Pyroclast Squad S.T | 92.87% | 89.09% | +3.78 pp | 93.67% | 64.47% |
| Terminator S.T | 92.85% | 89.15% | +3.70 pp | 93.34% | 65.55% |
| Exterminatus S.T | 89.01% | 92.31% | -3.30 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.33% | 92.36% | -2.03 pp | 92.65% | 4.24% |
| Infernus S.T | 92.02% | 90.78% | +1.24 pp | 93.15% | 61.53% |
| Whirlwind S.T | 91.20% | 92.27% | -1.07 pp | 93.46% | 47.82% |
| Servo-Skull | 91.24% | 91.70% | -0.45 pp | 91.30% | 21.95% |
| Demolitionist S.T | 91.64% | 91.50% | +0.14 pp | 93.28% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
