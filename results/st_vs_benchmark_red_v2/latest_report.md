# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 54,500
**S.T win rate:** 91.52%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.41% | 87.77% | +5.65 pp | 93.69% | 64.93% |
| Eradicator S.T | 93.19% | 88.44% | +4.75 pp | 95.07% | 58.12% |
| Drop Pod S.T | 93.00% | 88.69% | +4.31 pp | 93.32% | 57.52% |
| Pyroclast Squad S.T | 92.88% | 88.88% | +3.99 pp | 93.67% | 64.27% |
| Terminator S.T | 92.71% | 89.21% | +3.51 pp | 93.21% | 65.46% |
| Exterminatus S.T | 89.22% | 92.16% | -2.93 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.23% | 92.30% | -2.07 pp | 92.96% | 4.17% |
| Whirlwind S.T | 91.24% | 92.00% | -0.76 pp | 93.45% | 47.84% |
| Infernus S.T | 91.78% | 91.03% | +0.75 pp | 92.95% | 61.38% |
| Servo-Skull | 91.13% | 91.64% | -0.51 pp | 91.22% | 21.86% |
| Demolitionist S.T | 91.66% | 91.27% | +0.38 pp | 93.33% | 47.32% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
