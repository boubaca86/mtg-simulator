# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 9,000
**S.T win rate:** 92.03%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Eradicator S.T | 94.08% | 88.34% | +5.74 pp | 95.61% | 58.22% |
| Servitor | 93.88% | 88.32% | +5.55 pp | 94.12% | 65.33% |
| Pyroclast Squad S.T | 93.32% | 89.50% | +3.82 pp | 94.10% | 64.24% |
| Exterminatus S.T | 89.31% | 92.78% | -3.47 pp | 0.00% | 0.00% |
| Drop Pod S.T | 93.11% | 89.98% | +3.12 pp | 93.38% | 57.56% |
| Terminator S.T | 93.02% | 90.17% | +2.85 pp | 93.40% | 65.01% |
| Servo-Skull | 90.72% | 92.43% | -1.71 pp | 90.72% | 22.28% |
| Vindicator S.T | 91.12% | 92.58% | -1.46 pp | 93.40% | 4.38% |
| Infernus S.T | 92.38% | 91.38% | +1.00 pp | 93.41% | 61.18% |
| Whirlwind S.T | 91.81% | 92.40% | -0.59 pp | 93.53% | 47.74% |
| Demolitionist S.T | 92.05% | 92.01% | +0.04 pp | 93.67% | 47.60% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
