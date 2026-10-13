# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 100,000
**S.T win rate:** 91.54%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.57% | 87.55% | +6.02 pp | 93.83% | 64.83% |
| Eradicator S.T | 93.16% | 88.58% | +4.58 pp | 95.08% | 57.96% |
| Drop Pod S.T | 93.02% | 88.74% | +4.27 pp | 93.32% | 57.50% |
| Pyroclast Squad S.T | 92.86% | 88.98% | +3.88 pp | 93.67% | 64.31% |
| Terminator S.T | 92.82% | 89.06% | +3.76 pp | 93.31% | 65.53% |
| Exterminatus S.T | 89.06% | 92.23% | -3.16 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.24% | 92.33% | -2.09 pp | 92.59% | 4.24% |
| Whirlwind S.T | 91.13% | 92.25% | -1.12 pp | 93.37% | 47.92% |
| Infernus S.T | 91.92% | 90.82% | +1.11 pp | 93.08% | 61.49% |
| Servo-Skull | 91.18% | 91.65% | -0.47 pp | 91.24% | 21.98% |
| Demolitionist S.T | 91.60% | 91.43% | +0.17 pp | 93.22% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
