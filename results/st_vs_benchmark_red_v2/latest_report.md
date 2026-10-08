# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 44,500
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.70% | +5.72 pp | 93.71% | 65.09% |
| Eradicator S.T | 93.21% | 88.39% | +4.82 pp | 95.03% | 58.04% |
| Drop Pod S.T | 93.05% | 88.56% | +4.49 pp | 93.36% | 57.54% |
| Pyroclast Squad S.T | 92.88% | 88.85% | +4.03 pp | 93.68% | 64.22% |
| Terminator S.T | 92.62% | 89.34% | +3.27 pp | 93.13% | 65.50% |
| Exterminatus S.T | 89.13% | 92.16% | -3.03 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.34% | -2.20 pp | 92.71% | 4.10% |
| Infernus S.T | 91.80% | 90.96% | +0.83 pp | 92.96% | 61.43% |
| Whirlwind S.T | 91.21% | 92.02% | -0.81 pp | 93.50% | 47.76% |
| Demolitionist S.T | 91.69% | 91.17% | +0.51 pp | 93.50% | 47.18% |
| Servo-Skull | 91.17% | 91.60% | -0.43 pp | 91.25% | 21.74% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
