# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 124,500
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.71% | +5.84 pp | 93.83% | 64.97% |
| Eradicator S.T | 93.18% | 88.69% | +4.49 pp | 95.08% | 57.86% |
| Drop Pod S.T | 93.03% | 88.87% | +4.15 pp | 93.31% | 57.49% |
| Pyroclast Squad S.T | 92.86% | 89.11% | +3.74 pp | 93.65% | 64.49% |
| Terminator S.T | 92.86% | 89.12% | +3.74 pp | 93.36% | 65.53% |
| Exterminatus S.T | 89.00% | 92.31% | -3.31 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.33% | 92.36% | -2.03 pp | 92.68% | 4.24% |
| Infernus S.T | 92.02% | 90.78% | +1.25 pp | 93.15% | 61.55% |
| Whirlwind S.T | 91.19% | 92.29% | -1.10 pp | 93.45% | 47.84% |
| Servo-Skull | 91.23% | 91.70% | -0.47 pp | 91.29% | 21.95% |
| Demolitionist S.T | 91.65% | 91.49% | +0.15 pp | 93.27% | 47.25% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
