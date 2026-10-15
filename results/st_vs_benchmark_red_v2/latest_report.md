# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 132,500
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.55% | 87.70% | +5.85 pp | 93.82% | 65.01% |
| Eradicator S.T | 93.15% | 88.75% | +4.40 pp | 95.07% | 57.84% |
| Drop Pod S.T | 93.03% | 88.86% | +4.17 pp | 93.30% | 57.46% |
| Terminator S.T | 92.85% | 89.14% | +3.71 pp | 93.35% | 65.53% |
| Pyroclast Squad S.T | 92.83% | 89.16% | +3.68 pp | 93.64% | 64.42% |
| Exterminatus S.T | 88.98% | 92.31% | -3.34 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.31% | 92.37% | -2.06 pp | 92.70% | 4.24% |
| Infernus S.T | 92.02% | 90.78% | +1.24 pp | 93.15% | 61.58% |
| Whirlwind S.T | 91.20% | 92.27% | -1.07 pp | 93.49% | 47.87% |
| Servo-Skull | 91.23% | 91.69% | -0.47 pp | 91.28% | 21.94% |
| Demolitionist S.T | 91.65% | 91.47% | +0.18 pp | 93.29% | 47.24% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
