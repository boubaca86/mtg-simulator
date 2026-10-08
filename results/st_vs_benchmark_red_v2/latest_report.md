# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 45,500
**S.T win rate:** 91.51%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.38% | 87.77% | +5.61 pp | 93.67% | 65.09% |
| Eradicator S.T | 93.20% | 88.42% | +4.78 pp | 95.02% | 57.99% |
| Drop Pod S.T | 93.06% | 88.54% | +4.53 pp | 93.38% | 57.48% |
| Pyroclast Squad S.T | 92.87% | 88.86% | +4.02 pp | 93.68% | 64.23% |
| Terminator S.T | 92.63% | 89.31% | +3.32 pp | 93.14% | 65.50% |
| Exterminatus S.T | 89.14% | 92.16% | -3.02 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.17% | 92.32% | -2.15 pp | 92.83% | 4.11% |
| Infernus S.T | 91.80% | 90.95% | +0.86 pp | 92.99% | 61.47% |
| Whirlwind S.T | 91.21% | 92.01% | -0.80 pp | 93.50% | 47.73% |
| Demolitionist S.T | 91.69% | 91.18% | +0.51 pp | 93.49% | 47.24% |
| Servo-Skull | 91.15% | 91.61% | -0.46 pp | 91.23% | 21.71% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
