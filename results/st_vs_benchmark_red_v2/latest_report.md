# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 85,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.51% | 87.65% | +5.86 pp | 93.77% | 64.81% |
| Eradicator S.T | 93.13% | 88.61% | +4.52 pp | 95.09% | 58.06% |
| Drop Pod S.T | 93.00% | 88.74% | +4.26 pp | 93.31% | 57.62% |
| Pyroclast Squad S.T | 92.83% | 89.03% | +3.79 pp | 93.64% | 64.24% |
| Terminator S.T | 92.76% | 89.15% | +3.61 pp | 93.27% | 65.58% |
| Exterminatus S.T | 89.15% | 92.20% | -3.05 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.21% | 92.35% | -2.14 pp | 92.71% | 4.25% |
| Whirlwind S.T | 91.14% | 92.23% | -1.08 pp | 93.42% | 47.91% |
| Infernus S.T | 91.89% | 90.88% | +1.01 pp | 93.04% | 61.35% |
| Servo-Skull | 91.27% | 91.62% | -0.35 pp | 91.36% | 21.97% |
| Demolitionist S.T | 91.63% | 91.37% | +0.25 pp | 93.33% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
