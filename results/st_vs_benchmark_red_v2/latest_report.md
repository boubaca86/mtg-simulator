# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 119,000
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.56% | 87.68% | +5.88 pp | 93.84% | 64.96% |
| Eradicator S.T | 93.19% | 88.67% | +4.52 pp | 95.08% | 57.82% |
| Drop Pod S.T | 93.04% | 88.84% | +4.20 pp | 93.33% | 57.50% |
| Pyroclast Squad S.T | 92.86% | 89.11% | +3.75 pp | 93.65% | 64.46% |
| Terminator S.T | 92.84% | 89.14% | +3.70 pp | 93.33% | 65.58% |
| Exterminatus S.T | 89.05% | 92.29% | -3.25 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.36% | 92.34% | -1.99 pp | 92.73% | 4.25% |
| Infernus S.T | 92.02% | 90.77% | +1.24 pp | 93.16% | 61.54% |
| Whirlwind S.T | 91.19% | 92.27% | -1.08 pp | 93.44% | 47.82% |
| Servo-Skull | 91.21% | 91.70% | -0.49 pp | 91.27% | 22.00% |
| Demolitionist S.T | 91.64% | 91.49% | +0.15 pp | 93.29% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
