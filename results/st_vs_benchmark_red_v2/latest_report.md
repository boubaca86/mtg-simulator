# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 65,500
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.73% | +5.69 pp | 93.66% | 64.89% |
| Eradicator S.T | 93.16% | 88.45% | +4.71 pp | 95.06% | 58.15% |
| Drop Pod S.T | 93.00% | 88.65% | +4.35 pp | 93.31% | 57.61% |
| Pyroclast Squad S.T | 92.80% | 88.99% | +3.82 pp | 93.61% | 64.18% |
| Terminator S.T | 92.70% | 89.19% | +3.51 pp | 93.20% | 65.47% |
| Exterminatus S.T | 89.11% | 92.17% | -3.06 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.31% | -2.11 pp | 93.02% | 4.20% |
| Infernus S.T | 91.85% | 90.86% | +0.99 pp | 93.01% | 61.38% |
| Whirlwind S.T | 91.17% | 92.09% | -0.93 pp | 93.42% | 47.86% |
| Servo-Skull | 91.28% | 91.57% | -0.29 pp | 91.34% | 22.01% |
| Demolitionist S.T | 91.59% | 91.34% | +0.25 pp | 93.31% | 47.28% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
