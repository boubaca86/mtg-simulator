# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 159,000
**S.T win rate:** 91.57%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.50% | 87.76% | +5.74 pp | 93.79% | 65.00% |
| Eradicator S.T | 93.13% | 88.73% | +4.40 pp | 95.03% | 57.86% |
| Drop Pod S.T | 93.00% | 88.86% | +4.14 pp | 93.26% | 57.48% |
| Terminator S.T | 92.86% | 89.08% | +3.78 pp | 93.35% | 65.49% |
| Pyroclast Squad S.T | 92.84% | 89.10% | +3.74 pp | 93.63% | 64.41% |
| Exterminatus S.T | 88.95% | 92.30% | -3.36 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.26% | 92.38% | -2.11 pp | 92.87% | 4.22% |
| Infernus S.T | 92.01% | 90.75% | +1.26 pp | 93.14% | 61.53% |
| Whirlwind S.T | 91.17% | 92.28% | -1.12 pp | 93.49% | 47.80% |
| Servo-Skull | 91.19% | 91.69% | -0.50 pp | 91.23% | 22.04% |
| Demolitionist S.T | 91.68% | 91.39% | +0.29 pp | 93.32% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
