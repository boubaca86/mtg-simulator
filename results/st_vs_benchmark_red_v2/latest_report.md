# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 296,000
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.67% | +5.76 pp | 93.71% | 64.95% |
| Eradicator S.T | 93.05% | 88.67% | +4.38 pp | 94.99% | 57.76% |
| Drop Pod S.T | 92.92% | 88.79% | +4.14 pp | 93.17% | 57.41% |
| Terminator S.T | 92.79% | 88.97% | +3.82 pp | 93.29% | 65.49% |
| Pyroclast Squad S.T | 92.76% | 89.03% | +3.73 pp | 93.56% | 64.30% |
| Exterminatus S.T | 88.89% | 92.22% | -3.32 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.16% | 92.31% | -2.15 pp | 92.45% | 4.24% |
| Infernus S.T | 91.92% | 90.71% | +1.21 pp | 93.05% | 61.34% |
| Whirlwind S.T | 91.14% | 92.12% | -0.98 pp | 93.51% | 47.78% |
| Servo-Skull | 91.11% | 91.61% | -0.50 pp | 91.12% | 22.07% |
| Demolitionist S.T | 91.58% | 91.34% | +0.24 pp | 93.26% | 47.05% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
