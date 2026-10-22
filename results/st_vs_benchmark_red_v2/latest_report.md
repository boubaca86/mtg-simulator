# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 215,000
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.46% | 87.67% | +5.79 pp | 93.75% | 64.90% |
| Eradicator S.T | 93.07% | 88.68% | +4.39 pp | 94.98% | 57.83% |
| Drop Pod S.T | 92.96% | 88.77% | +4.20 pp | 93.21% | 57.46% |
| Terminator S.T | 92.81% | 89.00% | +3.81 pp | 93.31% | 65.44% |
| Pyroclast Squad S.T | 92.76% | 89.08% | +3.68 pp | 93.58% | 64.31% |
| Exterminatus S.T | 88.83% | 92.26% | -3.43 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.18% | 92.33% | -2.15 pp | 92.57% | 4.24% |
| Infernus S.T | 91.95% | 90.70% | +1.25 pp | 93.08% | 61.42% |
| Whirlwind S.T | 91.16% | 92.13% | -0.96 pp | 93.53% | 47.86% |
| Servo-Skull | 91.09% | 91.64% | -0.56 pp | 91.12% | 22.01% |
| Demolitionist S.T | 91.62% | 91.33% | +0.29 pp | 93.26% | 47.16% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
