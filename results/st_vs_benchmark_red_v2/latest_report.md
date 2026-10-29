# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 297,000
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.67% | +5.75 pp | 93.71% | 64.95% |
| Eradicator S.T | 93.04% | 88.66% | +4.38 pp | 94.99% | 57.77% |
| Drop Pod S.T | 92.92% | 88.79% | +4.14 pp | 93.17% | 57.40% |
| Terminator S.T | 92.79% | 88.96% | +3.83 pp | 93.29% | 65.49% |
| Pyroclast Squad S.T | 92.76% | 89.02% | +3.75 pp | 93.56% | 64.30% |
| Exterminatus S.T | 88.88% | 92.22% | -3.33 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.15% | 92.31% | -2.17 pp | 92.42% | 4.24% |
| Infernus S.T | 91.91% | 90.71% | +1.20 pp | 93.04% | 61.34% |
| Whirlwind S.T | 91.13% | 92.12% | -0.98 pp | 93.51% | 47.76% |
| Servo-Skull | 91.10% | 91.61% | -0.51 pp | 91.11% | 22.07% |
| Demolitionist S.T | 91.58% | 91.34% | +0.24 pp | 93.26% | 47.05% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
