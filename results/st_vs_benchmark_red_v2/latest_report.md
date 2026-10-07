# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 31,000
**S.T win rate:** 91.62%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.57% | 87.72% | +5.85 pp | 93.84% | 65.13% |
| Eradicator S.T | 93.31% | 88.49% | +4.82 pp | 95.07% | 58.45% |
| Drop Pod S.T | 93.06% | 88.84% | +4.22 pp | 93.36% | 57.86% |
| Pyroclast Squad S.T | 92.93% | 89.08% | +3.85 pp | 93.67% | 64.09% |
| Terminator S.T | 92.66% | 89.61% | +3.05 pp | 93.11% | 65.43% |
| Exterminatus S.T | 89.27% | 92.26% | -2.99 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.25% | 92.44% | -2.19 pp | 92.40% | 4.12% |
| Infernus S.T | 91.92% | 91.05% | +0.87 pp | 93.07% | 61.52% |
| Whirlwind S.T | 91.39% | 92.01% | -0.62 pp | 93.65% | 47.67% |
| Demolitionist S.T | 91.79% | 91.31% | +0.48 pp | 93.50% | 47.36% |
| Servo-Skull | 91.33% | 91.70% | -0.37 pp | 91.40% | 22.01% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
