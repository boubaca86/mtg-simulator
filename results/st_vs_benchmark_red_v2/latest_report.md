# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 58,000
**S.T win rate:** 91.48%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.40% | 87.70% | +5.70 pp | 93.66% | 64.85% |
| Eradicator S.T | 93.16% | 88.39% | +4.77 pp | 95.05% | 58.12% |
| Drop Pod S.T | 92.95% | 88.68% | +4.27 pp | 93.29% | 57.59% |
| Pyroclast Squad S.T | 92.82% | 88.89% | +3.93 pp | 93.59% | 64.22% |
| Terminator S.T | 92.67% | 89.17% | +3.51 pp | 93.16% | 65.48% |
| Exterminatus S.T | 89.10% | 92.14% | -3.04 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.18% | 92.28% | -2.10 pp | 92.90% | 4.18% |
| Whirlwind S.T | 91.16% | 92.04% | -0.89 pp | 93.41% | 47.85% |
| Infernus S.T | 91.78% | 90.93% | +0.85 pp | 92.93% | 61.37% |
| Servo-Skull | 91.18% | 91.57% | -0.39 pp | 91.27% | 21.91% |
| Demolitionist S.T | 91.59% | 91.28% | +0.32 pp | 93.27% | 47.38% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
