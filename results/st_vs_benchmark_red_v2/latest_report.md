# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 134,000
**S.T win rate:** 91.59%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.54% | 87.69% | +5.85 pp | 93.82% | 65.03% |
| Eradicator S.T | 93.14% | 88.76% | +4.38 pp | 95.06% | 57.86% |
| Drop Pod S.T | 93.03% | 88.85% | +4.18 pp | 93.30% | 57.44% |
| Terminator S.T | 92.85% | 89.12% | +3.72 pp | 93.35% | 65.52% |
| Pyroclast Squad S.T | 92.83% | 89.14% | +3.69 pp | 93.64% | 64.44% |
| Exterminatus S.T | 88.96% | 92.31% | -3.35 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.30% | 92.37% | -2.07 pp | 92.74% | 4.25% |
| Infernus S.T | 92.01% | 90.79% | +1.22 pp | 93.14% | 61.57% |
| Whirlwind S.T | 91.19% | 92.27% | -1.08 pp | 93.49% | 47.86% |
| Servo-Skull | 91.22% | 91.69% | -0.47 pp | 91.27% | 21.95% |
| Demolitionist S.T | 91.66% | 91.46% | +0.20 pp | 93.30% | 47.22% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
