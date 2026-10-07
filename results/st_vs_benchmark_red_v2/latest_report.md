# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 27,500
**S.T win rate:** 91.67%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.64% | 87.70% | +5.94 pp | 93.89% | 65.28% |
| Eradicator S.T | 93.35% | 88.57% | +4.78 pp | 95.07% | 58.39% |
| Drop Pod S.T | 93.08% | 88.95% | +4.13 pp | 93.39% | 57.64% |
| Pyroclast Squad S.T | 92.96% | 89.18% | +3.78 pp | 93.67% | 63.92% |
| Terminator S.T | 92.69% | 89.68% | +3.01 pp | 93.13% | 65.39% |
| Exterminatus S.T | 89.47% | 92.26% | -2.79 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.21% | 92.54% | -2.33 pp | 92.09% | 4.19% |
| Infernus S.T | 91.95% | 91.13% | +0.81 pp | 93.08% | 61.48% |
| Demolitionist S.T | 91.91% | 91.23% | +0.68 pp | 93.62% | 47.38% |
| Servo-Skull | 91.28% | 91.78% | -0.50 pp | 91.30% | 22.03% |
| Whirlwind S.T | 91.50% | 91.96% | -0.46 pp | 93.68% | 47.84% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
