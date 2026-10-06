# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 26,500
**S.T win rate:** 91.64%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.67% | 87.57% | +6.11 pp | 93.94% | 65.31% |
| Eradicator S.T | 93.28% | 88.63% | +4.65 pp | 95.02% | 58.39% |
| Drop Pod S.T | 93.07% | 88.92% | +4.15 pp | 93.37% | 57.53% |
| Pyroclast Squad S.T | 92.96% | 89.12% | +3.84 pp | 93.67% | 63.89% |
| Terminator S.T | 92.63% | 89.73% | +2.89 pp | 93.07% | 65.42% |
| Exterminatus S.T | 89.43% | 92.24% | -2.82 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.12% | 92.56% | -2.43 pp | 92.01% | 4.20% |
| Infernus S.T | 91.91% | 91.14% | +0.77 pp | 93.05% | 61.55% |
| Demolitionist S.T | 91.91% | 91.15% | +0.76 pp | 93.66% | 47.29% |
| Servo-Skull | 91.15% | 91.79% | -0.64 pp | 91.18% | 22.03% |
| Whirlwind S.T | 91.46% | 91.96% | -0.51 pp | 93.66% | 47.89% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
