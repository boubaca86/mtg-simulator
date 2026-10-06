# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 15,500
**S.T win rate:** 91.68%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.74% | 87.54% | +6.20 pp | 93.99% | 65.26% |
| Eradicator S.T | 93.52% | 88.33% | +5.19 pp | 95.14% | 58.27% |
| Drop Pod S.T | 92.97% | 89.20% | +3.77 pp | 93.31% | 57.63% |
| Pyroclast Squad S.T | 92.94% | 89.27% | +3.66 pp | 93.73% | 63.77% |
| Terminator S.T | 92.63% | 89.86% | +2.76 pp | 93.02% | 65.17% |
| Exterminatus S.T | 89.70% | 92.22% | -2.52 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.42% | 92.43% | -2.01 pp | 92.78% | 4.20% |
| Infernus S.T | 92.02% | 91.02% | +1.01 pp | 93.17% | 61.76% |
| Servo-Skull | 90.93% | 91.90% | -0.97 pp | 91.06% | 22.01% |
| Whirlwind S.T | 91.35% | 92.24% | -0.89 pp | 93.49% | 48.28% |
| Demolitionist S.T | 91.82% | 91.42% | +0.39 pp | 93.64% | 47.57% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
