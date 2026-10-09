# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 57,500
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.71% | +5.71 pp | 93.68% | 64.87% |
| Eradicator S.T | 93.17% | 88.42% | +4.75 pp | 95.06% | 58.13% |
| Drop Pod S.T | 92.97% | 88.68% | +4.29 pp | 93.30% | 57.58% |
| Pyroclast Squad S.T | 92.84% | 88.89% | +3.95 pp | 93.61% | 64.24% |
| Terminator S.T | 92.68% | 89.20% | +3.48 pp | 93.17% | 65.48% |
| Exterminatus S.T | 89.13% | 92.16% | -3.03 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.18% | 92.30% | -2.11 pp | 92.88% | 4.18% |
| Infernus S.T | 91.79% | 90.94% | +0.85 pp | 92.94% | 61.40% |
| Whirlwind S.T | 91.18% | 92.04% | -0.85 pp | 93.43% | 47.80% |
| Servo-Skull | 91.18% | 91.59% | -0.41 pp | 91.27% | 21.90% |
| Demolitionist S.T | 91.61% | 91.29% | +0.32 pp | 93.28% | 47.39% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
