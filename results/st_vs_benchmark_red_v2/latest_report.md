# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 3,000
**S.T win rate:** 92.80%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Eradicator S.T | 94.89% | 88.87% | +6.03 pp | 96.53% | 58.67% |
| Pyroclast Squad S.T | 94.36% | 89.65% | +4.72 pp | 95.02% | 64.90% |
| Exterminatus S.T | 89.91% | 93.59% | -3.68 pp | 0.00% | 0.00% |
| Servitor | 93.98% | 90.40% | +3.58 pp | 94.19% | 65.43% |
| Vindicator S.T | 91.08% | 93.79% | -2.71 pp | 92.96% | 4.73% |
| Servo-Skull | 90.84% | 93.38% | -2.54 pp | 90.73% | 22.30% |
| Drop Pod S.T | 93.64% | 91.20% | +2.44 pp | 93.96% | 57.93% |
| Demolitionist S.T | 93.56% | 91.41% | +2.15 pp | 94.96% | 48.27% |
| Terminator S.T | 93.53% | 91.45% | +2.08 pp | 93.91% | 64.57% |
| Infernus S.T | 93.11% | 92.23% | +0.87 pp | 94.01% | 61.17% |
| Whirlwind S.T | 93.00% | 92.45% | +0.55 pp | 94.56% | 48.37% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
