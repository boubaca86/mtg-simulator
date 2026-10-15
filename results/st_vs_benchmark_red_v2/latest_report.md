# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 131,000
**S.T win rate:** 91.58%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.69% | +5.85 pp | 93.82% | 65.00% |
| Eradicator S.T | 93.15% | 88.72% | +4.43 pp | 95.08% | 57.82% |
| Drop Pod S.T | 93.02% | 88.86% | +4.16 pp | 93.29% | 57.49% |
| Terminator S.T | 92.84% | 89.13% | +3.70 pp | 93.33% | 65.53% |
| Pyroclast Squad S.T | 92.83% | 89.14% | +3.69 pp | 93.63% | 64.44% |
| Exterminatus S.T | 89.00% | 92.30% | -3.30 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.31% | 92.36% | -2.05 pp | 92.68% | 4.23% |
| Infernus S.T | 92.01% | 90.77% | +1.24 pp | 93.14% | 61.58% |
| Whirlwind S.T | 91.20% | 92.24% | -1.04 pp | 93.49% | 47.88% |
| Servo-Skull | 91.24% | 91.68% | -0.43 pp | 91.29% | 21.93% |
| Demolitionist S.T | 91.64% | 91.47% | +0.18 pp | 93.28% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
