# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 241,500
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.61% | +5.82 pp | 93.72% | 64.93% |
| Eradicator S.T | 93.03% | 88.64% | +4.40 pp | 94.97% | 57.82% |
| Drop Pod S.T | 92.91% | 88.76% | +4.14 pp | 93.16% | 57.44% |
| Terminator S.T | 92.79% | 88.93% | +3.85 pp | 93.28% | 65.46% |
| Pyroclast Squad S.T | 92.72% | 89.05% | +3.67 pp | 93.53% | 64.28% |
| Exterminatus S.T | 88.80% | 92.22% | -3.41 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.14% | 92.29% | -2.15 pp | 92.47% | 4.23% |
| Infernus S.T | 91.91% | 90.67% | +1.24 pp | 93.03% | 61.40% |
| Whirlwind S.T | 91.11% | 92.11% | -1.00 pp | 93.49% | 47.85% |
| Servo-Skull | 91.06% | 91.60% | -0.53 pp | 91.09% | 22.06% |
| Demolitionist S.T | 91.57% | 91.31% | +0.26 pp | 93.22% | 47.07% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
