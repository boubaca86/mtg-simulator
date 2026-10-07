# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 35,000
**S.T win rate:** 91.49%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.43% | 87.62% | +5.81 pp | 93.72% | 65.01% |
| Eradicator S.T | 93.18% | 88.37% | +4.81 pp | 95.02% | 58.23% |
| Drop Pod S.T | 93.09% | 88.41% | +4.67 pp | 93.42% | 57.69% |
| Pyroclast Squad S.T | 92.84% | 88.87% | +3.97 pp | 93.62% | 64.13% |
| Terminator S.T | 92.52% | 89.47% | +3.05 pp | 93.00% | 65.55% |
| Exterminatus S.T | 89.13% | 92.14% | -3.01 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.25% | 92.24% | -1.99 pp | 92.79% | 4.16% |
| Infernus S.T | 91.86% | 90.78% | +1.08 pp | 93.06% | 61.47% |
| Whirlwind S.T | 91.23% | 91.94% | -0.71 pp | 93.58% | 47.65% |
| Demolitionist S.T | 91.69% | 91.12% | +0.57 pp | 93.47% | 47.27% |
| Servo-Skull | 91.14% | 91.59% | -0.45 pp | 91.22% | 22.00% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
