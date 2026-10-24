# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 239,500
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.45% | 87.60% | +5.85 pp | 93.73% | 64.94% |
| Eradicator S.T | 93.03% | 88.65% | +4.38 pp | 94.97% | 57.81% |
| Drop Pod S.T | 92.92% | 88.77% | +4.15 pp | 93.16% | 57.45% |
| Terminator S.T | 92.78% | 88.96% | +3.83 pp | 93.27% | 65.45% |
| Pyroclast Squad S.T | 92.73% | 89.05% | +3.68 pp | 93.54% | 64.28% |
| Exterminatus S.T | 88.80% | 92.23% | -3.43 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.14% | 92.30% | -2.16 pp | 92.46% | 4.24% |
| Infernus S.T | 91.91% | 90.68% | +1.24 pp | 93.04% | 61.41% |
| Whirlwind S.T | 91.12% | 92.12% | -1.00 pp | 93.49% | 47.85% |
| Servo-Skull | 91.07% | 91.61% | -0.54 pp | 91.09% | 22.06% |
| Demolitionist S.T | 91.58% | 91.31% | +0.27 pp | 93.23% | 47.09% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
