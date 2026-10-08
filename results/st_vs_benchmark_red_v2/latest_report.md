# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 48,000
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.45% | 87.80% | +5.66 pp | 93.74% | 65.03% |
| Eradicator S.T | 93.26% | 88.46% | +4.80 pp | 95.07% | 58.01% |
| Drop Pod S.T | 93.10% | 88.62% | +4.48 pp | 93.41% | 57.47% |
| Pyroclast Squad S.T | 92.91% | 88.94% | +3.97 pp | 93.70% | 64.27% |
| Terminator S.T | 92.73% | 89.29% | +3.44 pp | 93.24% | 65.57% |
| Exterminatus S.T | 89.18% | 92.22% | -3.04 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.37% | -2.14 pp | 92.86% | 4.11% |
| Infernus S.T | 91.85% | 91.01% | +0.85 pp | 93.02% | 61.48% |
| Whirlwind S.T | 91.28% | 92.05% | -0.77 pp | 93.54% | 47.74% |
| Demolitionist S.T | 91.74% | 91.23% | +0.51 pp | 93.47% | 47.28% |
| Servo-Skull | 91.20% | 91.67% | -0.46 pp | 91.28% | 21.75% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
