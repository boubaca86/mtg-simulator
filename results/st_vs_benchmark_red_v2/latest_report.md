# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 125,000
**S.T win rate:** 91.60%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.72% | +5.84 pp | 93.84% | 64.97% |
| Eradicator S.T | 93.18% | 88.72% | +4.47 pp | 95.08% | 57.84% |
| Drop Pod S.T | 93.03% | 88.89% | +4.14 pp | 93.32% | 57.50% |
| Terminator S.T | 92.87% | 89.13% | +3.74 pp | 93.37% | 65.53% |
| Pyroclast Squad S.T | 92.86% | 89.14% | +3.72 pp | 93.66% | 64.48% |
| Exterminatus S.T | 89.01% | 92.32% | -3.31 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.34% | 92.37% | -2.03 pp | 92.70% | 4.24% |
| Infernus S.T | 92.04% | 90.78% | +1.25 pp | 93.17% | 61.54% |
| Whirlwind S.T | 91.20% | 92.30% | -1.09 pp | 93.47% | 47.86% |
| Servo-Skull | 91.22% | 91.71% | -0.49 pp | 91.27% | 21.95% |
| Demolitionist S.T | 91.66% | 91.50% | +0.16 pp | 93.28% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
