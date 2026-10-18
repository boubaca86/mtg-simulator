# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 166,000
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.50% | 87.75% | +5.75 pp | 93.79% | 65.01% |
| Eradicator S.T | 93.15% | 88.72% | +4.43 pp | 95.05% | 57.83% |
| Drop Pod S.T | 93.01% | 88.86% | +4.15 pp | 93.27% | 57.48% |
| Terminator S.T | 92.85% | 89.10% | +3.76 pp | 93.34% | 65.49% |
| Pyroclast Squad S.T | 92.83% | 89.13% | +3.70 pp | 93.62% | 64.43% |
| Exterminatus S.T | 88.98% | 92.30% | -3.32 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.29% | 92.37% | -2.08 pp | 92.77% | 4.21% |
| Infernus S.T | 92.01% | 90.77% | +1.25 pp | 93.15% | 61.47% |
| Whirlwind S.T | 91.18% | 92.26% | -1.08 pp | 93.50% | 47.78% |
| Servo-Skull | 91.19% | 91.69% | -0.51 pp | 91.24% | 22.04% |
| Demolitionist S.T | 91.66% | 91.44% | +0.22 pp | 93.31% | 47.22% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
