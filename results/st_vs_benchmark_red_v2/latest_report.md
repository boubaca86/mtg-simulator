# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 94,000
**S.T win rate:** 91.56%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.57% | 87.61% | +5.96 pp | 93.83% | 64.83% |
| Eradicator S.T | 93.15% | 88.65% | +4.51 pp | 95.09% | 58.04% |
| Drop Pod S.T | 93.06% | 88.72% | +4.34 pp | 93.36% | 57.56% |
| Pyroclast Squad S.T | 92.88% | 89.00% | +3.88 pp | 93.69% | 64.30% |
| Terminator S.T | 92.80% | 89.15% | +3.66 pp | 93.31% | 65.55% |
| Exterminatus S.T | 89.05% | 92.26% | -3.21 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.27% | 92.36% | -2.09 pp | 92.78% | 4.24% |
| Whirlwind S.T | 91.17% | 92.24% | -1.07 pp | 93.44% | 47.89% |
| Infernus S.T | 91.93% | 90.88% | +1.05 pp | 93.08% | 61.41% |
| Servo-Skull | 91.26% | 91.65% | -0.39 pp | 91.33% | 21.96% |
| Demolitionist S.T | 91.63% | 91.44% | +0.18 pp | 93.29% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
