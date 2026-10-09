# S.T vs Benchmark Red — V2 Fair-Search AI

**Games:** 52,500
**S.T win rate:** 91.50%

## Fair-play checks

- Both decks use the same legal-action generator, evaluator, and one-ply action search.
- The evaluator uses its own hand and public zones only; opponent hidden-card identities and future library order are not consulted.
- Every run aborts instead of recording results if the legality checker encounters an impossible action.
- Combat searches multiple attack combinations; the defender uses the same public-information blocking heuristic for either deck.

## Card signals

| Card | Drawn WR | Not drawn WR | Delta | Cast WR | Cast rate |
|---|---:|---:|---:|---:|---:|
| Servitor | 93.42% | 87.70% | +5.72 pp | 93.69% | 64.93% |
| Eradicator S.T | 93.20% | 88.38% | +4.82 pp | 95.08% | 58.11% |
| Drop Pod S.T | 93.00% | 88.64% | +4.36 pp | 93.31% | 57.53% |
| Pyroclast Squad S.T | 92.86% | 88.86% | +3.99 pp | 93.65% | 64.27% |
| Terminator S.T | 92.68% | 89.19% | +3.49 pp | 93.20% | 65.48% |
| Exterminatus S.T | 89.17% | 92.14% | -2.97 pp | 0.00% | 0.00% |
| Vindicator S.T | 90.20% | 92.29% | -2.09 pp | 92.85% | 4.16% |
| Infernus S.T | 91.78% | 90.97% | +0.81 pp | 92.95% | 61.39% |
| Whirlwind S.T | 91.25% | 91.93% | -0.67 pp | 93.49% | 47.85% |
| Servo-Skull | 91.10% | 91.62% | -0.51 pp | 91.20% | 21.79% |
| Demolitionist S.T | 91.66% | 91.20% | +0.46 pp | 93.37% | 47.27% |

## Limits

- V2 is stronger and fairer than v1, but it is not a complete Magic stack/priority engine or expert-human-equivalent AI.
- Instant-speed response windows and some corner cases remain simplified.
- Do not rebalance an individual card until paired same-seed A/B replacement tests are added.
