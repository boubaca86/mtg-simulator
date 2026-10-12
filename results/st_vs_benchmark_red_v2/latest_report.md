# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 92,500
**S.T win rate:** 91.55%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.60% | +5.96 pp | 93.82% | 64.82% |
| Eradicator S.T | 93.14% | 88.64% | +4.51 pp | 95.10% | 58.05% |
| Drop Pod S.T | 93.04% | 88.71% | +4.33 pp | 93.35% | 57.57% |
| Pyroclast Squad S.T | 92.87% | 88.99% | +3.88 pp | 93.68% | 64.25% |
| Terminator S.T | 92.78% | 89.16% | +3.62 pp | 93.28% | 65.55% |
| Exterminatus S.T | 89.08% | 92.24% | -3.16 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.35% | -2.11 pp | 92.86% | 4.24% |
| Whirlwind S.T | 91.16% | 92.24% | -1.08 pp | 93.43% | 47.91% |
| Infernus S.T | 91.92% | 90.86% | +1.06 pp | 93.07% | 61.40% |
| Servo-Skull | 91.24% | 91.64% | -0.41 pp | 91.31% | 21.96% |
| Demolitionist S.T | 91.62% | 91.43% | +0.19 pp | 93.30% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
