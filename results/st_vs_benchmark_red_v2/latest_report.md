# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 28,000
**S.T win rate:** 91.64%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.60% | 87.71% | +5.89 pp | 93.85% | 65.30% |
| Eradicator S.T | 93.34% | 88.51% | +4.83 pp | 95.07% | 58.43% |
| Drop Pod S.T | 93.06% | 88.93% | +4.12 pp | 93.36% | 57.64% |
| Pyroclast Squad S.T | 92.95% | 89.15% | +3.80 pp | 93.67% | 63.94% |
| Terminator S.T | 92.71% | 89.58% | +3.14 pp | 93.15% | 65.34% |
| Exterminatus S.T | 89.44% | 92.24% | -2.80 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.52% | -2.34 pp | 92.12% | 4.17% |
| Infernus S.T | 91.92% | 91.13% | +0.79 pp | 93.05% | 61.51% |
| Demolitionist S.T | 91.91% | 91.17% | +0.74 pp | 93.60% | 47.32% |
| Servo-Skull | 91.25% | 91.76% | -0.51 pp | 91.25% | 22.04% |
| Whirlwind S.T | 91.50% | 91.89% | -0.40 pp | 93.70% | 47.77% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
