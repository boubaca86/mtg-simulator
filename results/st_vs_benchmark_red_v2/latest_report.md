# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 83,500
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.64% | +5.90 pp | 93.79% | 64.84% |
| Eradicator S.T | 93.14% | 88.63% | +4.51 pp | 95.08% | 58.09% |
| Drop Pod S.T | 93.03% | 88.73% | +4.30 pp | 93.35% | 57.62% |
| Pyroclast Squad S.T | 92.82% | 89.08% | +3.74 pp | 93.64% | 64.24% |
| Terminator S.T | 92.79% | 89.13% | +3.66 pp | 93.31% | 65.58% |
| Exterminatus S.T | 89.18% | 92.21% | -3.03 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.36% | -2.13 pp | 92.78% | 4.23% |
| Whirlwind S.T | 91.15% | 92.25% | -1.10 pp | 93.43% | 47.90% |
| Infernus S.T | 91.91% | 90.88% | +1.02 pp | 93.06% | 61.33% |
| Servo-Skull | 91.30% | 91.62% | -0.32 pp | 91.38% | 21.93% |
| Demolitionist S.T | 91.63% | 91.40% | +0.24 pp | 93.33% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
