# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 9,500
**S.T win rate:** 92.02%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.88% | 88.27% | +5.61 pp | 94.11% | 65.44% |
| Eradicator S.T | 93.90% | 88.64% | +5.26 pp | 95.49% | 58.06% |
| Pyroclast Squad S.T | 93.28% | 89.55% | +3.72 pp | 94.09% | 64.28% |
| Exterminatus S.T | 89.47% | 92.72% | -3.25 pp | 0.00% | 0.00% |
| Drop Pod S.T | 93.11% | 89.94% | +3.17 pp | 93.39% | 57.65% |
| Terminator S.T | 92.96% | 90.23% | +2.73 pp | 93.36% | 65.12% |
| Vindicator S.T | 91.09% | 92.58% | -1.48 pp | 93.64% | 4.31% |
| Servo-Skull | 90.89% | 92.36% | -1.47 pp | 90.87% | 22.25% |
| Infernus S.T | 92.45% | 91.22% | +1.23 pp | 93.52% | 61.11% |
| Whirlwind S.T | 91.72% | 92.53% | -0.81 pp | 93.55% | 47.95% |
| Demolitionist S.T | 92.10% | 91.88% | +0.22 pp | 93.81% | 47.45% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
