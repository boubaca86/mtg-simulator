# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 7,500
**S.T win rate:** 92.35%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 94.19% | 88.63% | +5.56 pp | 94.41% | 65.36% |
| Eradicator S.T | 94.26% | 88.85% | +5.42 pp | 95.73% | 58.43% |
| Pyroclast Squad S.T | 93.69% | 89.69% | +4.00 pp | 94.43% | 64.36% |
| Exterminatus S.T | 89.54% | 93.12% | -3.58 pp | 0.00% | 0.00% |
| Drop Pod S.T | 93.36% | 90.40% | +2.96 pp | 93.63% | 57.60% |
| Terminator S.T | 93.20% | 90.72% | +2.49 pp | 93.52% | 65.23% |
| Servo-Skull | 90.49% | 92.90% | -2.41 pp | 90.54% | 22.28% |
| Vindicator S.T | 91.35% | 92.94% | -1.59 pp | 93.40% | 4.24% |
| Infernus S.T | 92.55% | 91.96% | +0.59 pp | 93.53% | 61.24% |
| Whirlwind S.T | 92.18% | 92.63% | -0.45 pp | 93.80% | 47.96% |
| Demolitionist S.T | 92.49% | 92.09% | +0.40 pp | 94.07% | 47.41% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
