# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 248,000
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.62% | +5.82 pp | 93.72% | 64.95% |
| Eradicator S.T | 93.03% | 88.65% | +4.38 pp | 94.98% | 57.80% |
| Drop Pod S.T | 92.91% | 88.78% | +4.13 pp | 93.16% | 57.41% |
| Terminator S.T | 92.80% | 88.93% | +3.86 pp | 93.29% | 65.47% |
| Pyroclast Squad S.T | 92.73% | 89.06% | +3.67 pp | 93.54% | 64.27% |
| Exterminatus S.T | 88.83% | 92.22% | -3.38 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.13% | 92.31% | -2.19 pp | 92.40% | 4.24% |
| Infernus S.T | 91.92% | 90.66% | +1.26 pp | 93.04% | 61.38% |
| Whirlwind S.T | 91.11% | 92.14% | -1.03 pp | 93.49% | 47.84% |
| Servo-Skull | 91.06% | 91.61% | -0.54 pp | 91.09% | 22.06% |
| Demolitionist S.T | 91.57% | 91.32% | +0.25 pp | 93.24% | 47.07% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
