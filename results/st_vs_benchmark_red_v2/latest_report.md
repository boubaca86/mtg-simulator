# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 298,000
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.67% | +5.75 pp | 93.71% | 64.96% |
| Eradicator S.T | 93.05% | 88.67% | +4.38 pp | 94.99% | 57.76% |
| Drop Pod S.T | 92.93% | 88.79% | +4.13 pp | 93.17% | 57.40% |
| Terminator S.T | 92.80% | 88.97% | +3.83 pp | 93.29% | 65.49% |
| Pyroclast Squad S.T | 92.77% | 89.02% | +3.75 pp | 93.56% | 64.30% |
| Exterminatus S.T | 88.89% | 92.22% | -3.33 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.15% | 92.32% | -2.16 pp | 92.41% | 4.24% |
| Infernus S.T | 91.92% | 90.71% | +1.21 pp | 93.05% | 61.34% |
| Whirlwind S.T | 91.14% | 92.12% | -0.98 pp | 93.52% | 47.76% |
| Servo-Skull | 91.10% | 91.61% | -0.51 pp | 91.12% | 22.07% |
| Demolitionist S.T | 91.58% | 91.35% | +0.23 pp | 93.26% | 47.05% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
