# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 33,000
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.53% | 87.61% | +5.92 pp | 93.79% | 65.11% |
| Eradicator S.T | 93.24% | 88.44% | +4.80 pp | 95.02% | 58.38% |
| Drop Pod S.T | 93.07% | 88.63% | +4.44 pp | 93.39% | 57.75% |
| Pyroclast Squad S.T | 92.91% | 88.94% | +3.98 pp | 93.66% | 64.15% |
| Terminator S.T | 92.60% | 89.53% | +3.07 pp | 93.06% | 65.48% |
| Exterminatus S.T | 89.17% | 92.21% | -3.03 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.38% | -2.19 pp | 92.67% | 4.09% |
| Infernus S.T | 91.89% | 90.92% | +0.97 pp | 93.05% | 61.49% |
| Whirlwind S.T | 91.32% | 91.97% | -0.65 pp | 93.60% | 47.76% |
| Demolitionist S.T | 91.73% | 91.24% | +0.49 pp | 93.48% | 47.29% |
| Servo-Skull | 91.33% | 91.62% | -0.30 pp | 91.40% | 21.95% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
