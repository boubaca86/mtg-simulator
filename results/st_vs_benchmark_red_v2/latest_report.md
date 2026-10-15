# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 133,500
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.68% | +5.87 pp | 93.83% | 65.02% |
| Eradicator S.T | 93.14% | 88.76% | +4.38 pp | 95.06% | 57.85% |
| Drop Pod S.T | 93.03% | 88.85% | +4.18 pp | 93.30% | 57.44% |
| Terminator S.T | 92.85% | 89.13% | +3.72 pp | 93.35% | 65.52% |
| Pyroclast Squad S.T | 92.83% | 89.15% | +3.69 pp | 93.64% | 64.44% |
| Exterminatus S.T | 88.96% | 92.32% | -3.35 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.31% | 92.37% | -2.06 pp | 92.76% | 4.24% |
| Infernus S.T | 92.02% | 90.78% | +1.24 pp | 93.16% | 61.57% |
| Whirlwind S.T | 91.20% | 92.27% | -1.07 pp | 93.50% | 47.86% |
| Servo-Skull | 91.24% | 91.69% | -0.46 pp | 91.29% | 21.94% |
| Demolitionist S.T | 91.66% | 91.46% | +0.19 pp | 93.30% | 47.22% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
