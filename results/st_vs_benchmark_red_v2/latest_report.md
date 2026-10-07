# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 32,500
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.57% | 87.63% | +5.94 pp | 93.83% | 65.12% |
| Eradicator S.T | 93.28% | 88.47% | +4.81 pp | 95.05% | 58.40% |
| Drop Pod S.T | 93.08% | 88.71% | +4.37 pp | 93.40% | 57.77% |
| Pyroclast Squad S.T | 92.92% | 89.01% | +3.91 pp | 93.68% | 64.12% |
| Terminator S.T | 92.61% | 89.60% | +3.01 pp | 93.08% | 65.49% |
| Exterminatus S.T | 89.30% | 92.21% | -2.91 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.19% | 92.43% | -2.24 pp | 92.68% | 4.12% |
| Infernus S.T | 91.92% | 90.96% | +0.96 pp | 93.08% | 61.53% |
| Whirlwind S.T | 91.35% | 91.99% | -0.64 pp | 93.64% | 47.72% |
| Demolitionist S.T | 91.77% | 91.26% | +0.51 pp | 93.49% | 47.29% |
| Servo-Skull | 91.30% | 91.67% | -0.38 pp | 91.37% | 22.00% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
