# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 83,000
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.53% | 87.66% | +5.87 pp | 93.79% | 64.86% |
| Eradicator S.T | 93.14% | 88.65% | +4.50 pp | 95.08% | 58.09% |
| Drop Pod S.T | 93.04% | 88.73% | +4.31 pp | 93.36% | 57.62% |
| Pyroclast Squad S.T | 92.84% | 89.07% | +3.77 pp | 93.65% | 64.24% |
| Terminator S.T | 92.78% | 89.15% | +3.63 pp | 93.30% | 65.58% |
| Exterminatus S.T | 89.19% | 92.21% | -3.03 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.37% | -2.14 pp | 92.80% | 4.23% |
| Whirlwind S.T | 91.15% | 92.27% | -1.13 pp | 93.43% | 47.92% |
| Infernus S.T | 91.92% | 90.87% | +1.06 pp | 93.08% | 61.32% |
| Servo-Skull | 91.30% | 91.63% | -0.33 pp | 91.38% | 21.91% |
| Demolitionist S.T | 91.65% | 91.39% | +0.26 pp | 93.33% | 47.23% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
