# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 88,000
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.53% | 87.58% | +5.95 pp | 93.79% | 64.81% |
| Eradicator S.T | 93.12% | 88.59% | +4.54 pp | 95.09% | 58.07% |
| Drop Pod S.T | 93.01% | 88.70% | +4.31 pp | 93.32% | 57.60% |
| Pyroclast Squad S.T | 92.82% | 88.99% | +3.83 pp | 93.64% | 64.27% |
| Terminator S.T | 92.73% | 89.16% | +3.58 pp | 93.25% | 65.56% |
| Exterminatus S.T | 89.08% | 92.20% | -3.12 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.34% | -2.14 pp | 92.73% | 4.24% |
| Whirlwind S.T | 91.13% | 92.20% | -1.07 pp | 93.43% | 47.90% |
| Infernus S.T | 91.88% | 90.85% | +1.03 pp | 93.05% | 61.36% |
| Servo-Skull | 91.25% | 91.60% | -0.35 pp | 91.33% | 21.95% |
| Demolitionist S.T | 91.59% | 91.39% | +0.20 pp | 93.31% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
