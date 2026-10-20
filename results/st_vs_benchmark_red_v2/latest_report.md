# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 188,000
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.48% | 87.67% | +5.81 pp | 93.76% | 64.93% |
| Eradicator S.T | 93.09% | 88.66% | +4.43 pp | 95.00% | 57.87% |
| Drop Pod S.T | 92.96% | 88.81% | +4.15 pp | 93.22% | 57.46% |
| Terminator S.T | 92.79% | 89.07% | +3.73 pp | 93.29% | 65.46% |
| Pyroclast Squad S.T | 92.78% | 89.07% | +3.71 pp | 93.58% | 64.38% |
| Exterminatus S.T | 88.88% | 92.26% | -3.38 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.17% | 92.36% | -2.19 pp | 92.58% | 4.21% |
| Infernus S.T | 91.95% | 90.73% | +1.23 pp | 93.10% | 61.45% |
| Whirlwind S.T | 91.16% | 92.16% | -0.99 pp | 93.51% | 47.85% |
| Servo-Skull | 91.08% | 91.66% | -0.58 pp | 91.10% | 22.02% |
| Demolitionist S.T | 91.62% | 91.36% | +0.27 pp | 93.23% | 47.19% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
