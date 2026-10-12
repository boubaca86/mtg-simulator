# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 92,000
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.60% | +5.96 pp | 93.81% | 64.81% |
| Eradicator S.T | 93.14% | 88.63% | +4.50 pp | 95.10% | 58.06% |
| Drop Pod S.T | 93.04% | 88.70% | +4.35 pp | 93.35% | 57.59% |
| Pyroclast Squad S.T | 92.86% | 88.99% | +3.88 pp | 93.68% | 64.25% |
| Terminator S.T | 92.77% | 89.17% | +3.60 pp | 93.27% | 65.55% |
| Exterminatus S.T | 89.05% | 92.24% | -3.19 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.35% | -2.12 pp | 92.85% | 4.23% |
| Whirlwind S.T | 91.15% | 92.24% | -1.09 pp | 93.42% | 47.93% |
| Infernus S.T | 91.91% | 90.86% | +1.06 pp | 93.07% | 61.39% |
| Servo-Skull | 91.23% | 91.64% | -0.41 pp | 91.30% | 21.95% |
| Demolitionist S.T | 91.61% | 91.43% | +0.18 pp | 93.29% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
