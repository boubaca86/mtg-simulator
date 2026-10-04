# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 1,500
**S.T win rate:** 93.20%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Pyroclast Squad S.T | 94.82% | 89.71% | +5.12 pp | 95.30% | 66.73% |
| Eradicator S.T | 94.83% | 90.06% | +4.77 pp | 96.81% | 58.47% |
| Vindicator S.T | 90.63% | 94.71% | -4.08 pp | 94.12% | 4.53% |
| Exterminatus S.T | 90.06% | 94.09% | -4.03 pp | 0.00% | 0.00% |
| Terminator S.T | 94.00% | 91.76% | +2.24 pp | 94.39% | 64.13% |
| Servitor | 93.91% | 91.77% | +2.15 pp | 94.29% | 65.33% |
| Drop Pod S.T | 93.85% | 92.00% | +1.85 pp | 94.07% | 57.33% |
| Whirlwind S.T | 92.62% | 94.19% | -1.57 pp | 94.76% | 48.33% |
| Servo-Skull | 92.11% | 93.54% | -1.42 pp | 92.06% | 22.67% |
| Demolitionist S.T | 93.55% | 92.60% | +0.95 pp | 95.38% | 47.60% |
| Infernus S.T | 93.54% | 92.59% | +0.95 pp | 94.15% | 60.40% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
