# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 55,500
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.41% | 87.72% | +5.70 pp | 93.68% | 64.91% |
| Eradicator S.T | 93.17% | 88.42% | +4.75 pp | 95.05% | 58.11% |
| Drop Pod S.T | 92.98% | 88.66% | +4.32 pp | 93.30% | 57.58% |
| Pyroclast Squad S.T | 92.85% | 88.88% | +3.97 pp | 93.63% | 64.28% |
| Terminator S.T | 92.69% | 89.19% | +3.50 pp | 93.19% | 65.43% |
| Exterminatus S.T | 89.14% | 92.15% | -3.01 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.22% | 92.28% | -2.05 pp | 92.95% | 4.19% |
| Whirlwind S.T | 91.21% | 92.00% | -0.80 pp | 93.44% | 47.82% |
| Infernus S.T | 91.76% | 91.01% | +0.74 pp | 92.92% | 61.37% |
| Servo-Skull | 91.12% | 91.61% | -0.49 pp | 91.21% | 21.87% |
| Demolitionist S.T | 91.62% | 91.27% | +0.35 pp | 93.29% | 47.37% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
