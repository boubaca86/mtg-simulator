# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 64,000
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.41% | 87.76% | +5.66 pp | 93.66% | 64.93% |
| Eradicator S.T | 93.16% | 88.47% | +4.69 pp | 95.07% | 58.14% |
| Drop Pod S.T | 93.00% | 88.67% | +4.33 pp | 93.33% | 57.58% |
| Pyroclast Squad S.T | 92.82% | 88.97% | +3.85 pp | 93.63% | 64.17% |
| Terminator S.T | 92.70% | 89.20% | +3.51 pp | 93.21% | 65.46% |
| Exterminatus S.T | 89.15% | 92.17% | -3.02 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.30% | -2.08 pp | 93.05% | 4.20% |
| Infernus S.T | 91.85% | 90.89% | +0.96 pp | 93.00% | 61.34% |
| Whirlwind S.T | 91.19% | 92.07% | -0.88 pp | 93.45% | 47.87% |
| Servo-Skull | 91.29% | 91.58% | -0.28 pp | 91.36% | 22.00% |
| Demolitionist S.T | 91.61% | 91.34% | +0.27 pp | 93.29% | 47.33% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
