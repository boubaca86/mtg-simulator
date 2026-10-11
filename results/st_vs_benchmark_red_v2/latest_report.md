# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 76,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.50% | 87.68% | +5.82 pp | 93.74% | 64.95% |
| Eradicator S.T | 93.16% | 88.57% | +4.59 pp | 95.08% | 58.08% |
| Drop Pod S.T | 93.05% | 88.66% | +4.39 pp | 93.34% | 57.64% |
| Pyroclast Squad S.T | 92.84% | 89.04% | +3.80 pp | 93.66% | 64.16% |
| Terminator S.T | 92.78% | 89.13% | +3.64 pp | 93.29% | 65.63% |
| Exterminatus S.T | 89.28% | 92.17% | -2.89 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.37% | -2.18 pp | 92.77% | 4.20% |
| Whirlwind S.T | 91.16% | 92.20% | -1.04 pp | 93.45% | 47.83% |
| Infernus S.T | 91.89% | 90.90% | +0.99 pp | 93.03% | 61.37% |
| Servo-Skull | 91.29% | 91.62% | -0.33 pp | 91.36% | 21.96% |
| Demolitionist S.T | 91.65% | 91.35% | +0.30 pp | 93.32% | 47.29% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
