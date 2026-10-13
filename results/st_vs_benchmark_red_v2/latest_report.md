# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 101,500
**S.T win rate:** 91.53%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.57% | 87.53% | +6.04 pp | 93.83% | 64.82% |
| Eradicator S.T | 93.15% | 88.57% | +4.58 pp | 95.06% | 57.94% |
| Drop Pod S.T | 93.01% | 88.73% | +4.28 pp | 93.31% | 57.49% |
| Pyroclast Squad S.T | 92.86% | 88.95% | +3.91 pp | 93.66% | 64.32% |
| Terminator S.T | 92.81% | 89.05% | +3.76 pp | 93.30% | 65.51% |
| Exterminatus S.T | 89.08% | 92.21% | -3.13 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.33% | -2.09 pp | 92.65% | 4.24% |
| Whirlwind S.T | 91.12% | 92.26% | -1.14 pp | 93.34% | 47.90% |
| Infernus S.T | 91.92% | 90.80% | +1.12 pp | 93.07% | 61.50% |
| Servo-Skull | 91.18% | 91.64% | -0.46 pp | 91.24% | 21.99% |
| Demolitionist S.T | 91.58% | 91.44% | +0.15 pp | 93.21% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
