# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 93,000
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.57% | 87.60% | +5.96 pp | 93.83% | 64.82% |
| Eradicator S.T | 93.15% | 88.65% | +4.50 pp | 95.10% | 58.03% |
| Drop Pod S.T | 93.05% | 88.72% | +4.32 pp | 93.36% | 57.57% |
| Pyroclast Squad S.T | 92.88% | 88.99% | +3.89 pp | 93.68% | 64.27% |
| Terminator S.T | 92.79% | 89.16% | +3.63 pp | 93.29% | 65.55% |
| Exterminatus S.T | 89.08% | 92.25% | -3.17 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.25% | 92.36% | -2.11 pp | 92.84% | 4.24% |
| Whirlwind S.T | 91.16% | 92.24% | -1.08 pp | 93.44% | 47.90% |
| Infernus S.T | 91.91% | 90.89% | +1.03 pp | 93.07% | 61.40% |
| Servo-Skull | 91.24% | 91.65% | -0.40 pp | 91.32% | 21.96% |
| Demolitionist S.T | 91.62% | 91.44% | +0.18 pp | 93.29% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
