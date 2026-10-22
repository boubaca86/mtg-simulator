# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 210,500
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.47% | 87.67% | +5.80 pp | 93.75% | 64.92% |
| Eradicator S.T | 93.07% | 88.69% | +4.38 pp | 94.98% | 57.83% |
| Drop Pod S.T | 92.96% | 88.79% | +4.17 pp | 93.21% | 57.45% |
| Terminator S.T | 92.82% | 89.00% | +3.81 pp | 93.31% | 65.45% |
| Pyroclast Squad S.T | 92.77% | 89.08% | +3.69 pp | 93.59% | 64.31% |
| Exterminatus S.T | 88.85% | 92.26% | -3.40 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.16% | 92.35% | -2.20 pp | 92.58% | 4.23% |
| Infernus S.T | 91.95% | 90.71% | +1.24 pp | 93.08% | 61.42% |
| Whirlwind S.T | 91.17% | 92.14% | -0.97 pp | 93.52% | 47.86% |
| Servo-Skull | 91.07% | 91.65% | -0.58 pp | 91.11% | 21.99% |
| Demolitionist S.T | 91.62% | 91.33% | +0.29 pp | 93.25% | 47.16% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
