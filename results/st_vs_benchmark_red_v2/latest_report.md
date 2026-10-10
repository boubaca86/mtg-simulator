# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 74,500
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.50% | 87.68% | +5.82 pp | 93.74% | 64.94% |
| Eradicator S.T | 93.16% | 88.57% | +4.59 pp | 95.08% | 58.09% |
| Drop Pod S.T | 93.04% | 88.67% | +4.37 pp | 93.34% | 57.62% |
| Pyroclast Squad S.T | 92.85% | 89.01% | +3.84 pp | 93.67% | 64.15% |
| Terminator S.T | 92.77% | 89.14% | +3.63 pp | 93.29% | 65.61% |
| Exterminatus S.T | 89.29% | 92.17% | -2.87 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.35% | -2.13 pp | 92.78% | 4.20% |
| Whirlwind S.T | 91.16% | 92.20% | -1.04 pp | 93.44% | 47.79% |
| Infernus S.T | 91.89% | 90.89% | +0.99 pp | 93.04% | 61.36% |
| Servo-Skull | 91.29% | 91.62% | -0.33 pp | 91.36% | 21.97% |
| Demolitionist S.T | 91.66% | 91.33% | +0.32 pp | 93.33% | 47.26% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
