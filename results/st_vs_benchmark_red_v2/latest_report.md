# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 45,000
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.39% | 87.74% | +5.66 pp | 93.69% | 65.10% |
| Eradicator S.T | 93.20% | 88.41% | +4.78 pp | 95.01% | 58.00% |
| Drop Pod S.T | 93.05% | 88.56% | +4.49 pp | 93.36% | 57.52% |
| Pyroclast Squad S.T | 92.88% | 88.85% | +4.03 pp | 93.68% | 64.22% |
| Terminator S.T | 92.63% | 89.32% | +3.31 pp | 93.14% | 65.53% |
| Exterminatus S.T | 89.15% | 92.15% | -3.00 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.17% | 92.31% | -2.14 pp | 92.77% | 4.12% |
| Infernus S.T | 91.80% | 90.96% | +0.84 pp | 92.97% | 61.43% |
| Whirlwind S.T | 91.22% | 92.00% | -0.78 pp | 93.50% | 47.78% |
| Demolitionist S.T | 91.70% | 91.15% | +0.55 pp | 93.49% | 47.25% |
| Servo-Skull | 91.14% | 91.61% | -0.47 pp | 91.22% | 21.72% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
