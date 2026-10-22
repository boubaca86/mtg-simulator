# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 208,000
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.47% | 87.67% | +5.80 pp | 93.75% | 64.93% |
| Eradicator S.T | 93.08% | 88.68% | +4.39 pp | 95.00% | 57.83% |
| Drop Pod S.T | 92.96% | 88.80% | +4.16 pp | 93.22% | 57.43% |
| Terminator S.T | 92.81% | 89.03% | +3.79 pp | 93.31% | 65.45% |
| Pyroclast Squad S.T | 92.77% | 89.09% | +3.68 pp | 93.58% | 64.31% |
| Exterminatus S.T | 88.87% | 92.26% | -3.39 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.17% | 92.36% | -2.19 pp | 92.65% | 4.23% |
| Infernus S.T | 91.95% | 90.73% | +1.21 pp | 93.07% | 61.43% |
| Whirlwind S.T | 91.17% | 92.14% | -0.97 pp | 93.53% | 47.87% |
| Servo-Skull | 91.09% | 91.65% | -0.57 pp | 91.12% | 21.99% |
| Demolitionist S.T | 91.63% | 91.33% | +0.30 pp | 93.26% | 47.16% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
