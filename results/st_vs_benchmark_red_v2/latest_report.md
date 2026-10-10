# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 75,000
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.49% | 87.71% | +5.78 pp | 93.73% | 64.95% |
| Eradicator S.T | 93.16% | 88.58% | +4.58 pp | 95.08% | 58.09% |
| Drop Pod S.T | 93.06% | 88.66% | +4.40 pp | 93.35% | 57.64% |
| Pyroclast Squad S.T | 92.85% | 89.03% | +3.82 pp | 93.66% | 64.14% |
| Terminator S.T | 92.79% | 89.12% | +3.66 pp | 93.30% | 65.62% |
| Exterminatus S.T | 89.30% | 92.17% | -2.87 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.36% | -2.13 pp | 92.77% | 4.20% |
| Whirlwind S.T | 91.17% | 92.21% | -1.04 pp | 93.45% | 47.81% |
| Infernus S.T | 91.90% | 90.89% | +1.01 pp | 93.05% | 61.36% |
| Servo-Skull | 91.29% | 91.62% | -0.34 pp | 91.36% | 21.98% |
| Demolitionist S.T | 91.66% | 91.35% | +0.31 pp | 93.34% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
