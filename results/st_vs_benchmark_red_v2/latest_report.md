# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 89,000
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.53% | 87.58% | +5.94 pp | 93.78% | 64.82% |
| Eradicator S.T | 93.13% | 88.57% | +4.56 pp | 95.09% | 58.06% |
| Drop Pod S.T | 93.00% | 88.70% | +4.30 pp | 93.31% | 57.58% |
| Pyroclast Squad S.T | 92.84% | 88.97% | +3.87 pp | 93.65% | 64.26% |
| Terminator S.T | 92.75% | 89.13% | +3.62 pp | 93.26% | 65.56% |
| Exterminatus S.T | 89.07% | 92.21% | -3.13 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.18% | 92.34% | -2.16 pp | 92.71% | 4.24% |
| Whirlwind S.T | 91.14% | 92.20% | -1.06 pp | 93.41% | 47.89% |
| Infernus S.T | 91.89% | 90.83% | +1.06 pp | 93.05% | 61.38% |
| Servo-Skull | 91.25% | 91.60% | -0.35 pp | 91.33% | 21.95% |
| Demolitionist S.T | 91.60% | 91.39% | +0.21 pp | 93.30% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
